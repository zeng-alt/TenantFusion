package com.github.zeng.alt.rest.apt.generator;

import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.github.zeng.alt.rest.apt.meta.SchemaFieldMeta;
import com.squareup.javapoet.*;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.*;

import static com.github.zeng.alt.rest.apt.generator.GeneratorUtils.*;

/**
 * Patch Mapper 生成器 — 为 PATCH 请求生成 MapStruct Mapper 接口。
 * <p>与 {@link MapperGenerator} 不同，Patch Mapper 使用
 * {@code @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)} 确保只更新非 null 字段，
 * 避免 PATCH 操作覆盖已有的非空值。</p>
 *
 * @author zengJiaJun
 * @since 2026年07月07日
 * @version 1.0
 */
public final class PatchMapperGenerator {

    private static final ClassName MAPPER = ClassName.get("org.mapstruct", "Mapper");
    private static final ClassName REPORTING_POLICY = ClassName.get("org.mapstruct", "ReportingPolicy");
    private static final ClassName MAPPING_TARGET = ClassName.get("org.mapstruct", "MappingTarget");
    private static final ClassName BEAN_MAPPING = ClassName.get("org.mapstruct", "BeanMapping");
    private static final ClassName NULL_VALUE_PROPERTY_MAPPING_STRATEGY =
            ClassName.get("org.mapstruct", "NullValuePropertyMappingStrategy");
    private static final ClassName OPTIONAL = ClassName.get("java.util", "Optional");

    private PatchMapperGenerator() {}

    /**
     * 为指定 Repository 元模型生成 Patch Mapper 接口。
     *
     * @param meta  Repository 元模型
     * @param elements APT Elements 工具，用于递归解析嵌套类型字段；可 null（降级为单层转换）
     * @return JavaFile 表示生成的 Patch Mapper 接口
     */
    public static JavaFile generate(RepositoryMeta meta, Elements elements) {
        String name = meta.getEntitySimpleName() + "PatchMapper";

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(name)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", "rest-apt-component")
                        .build())
                .addAnnotation(AnnotationSpec.builder(MAPPER)
                        .addMember("componentModel", "$S", "spring")
                        .addMember("unmappedTargetPolicy", "$T.$L", REPORTING_POLICY, "IGNORE")
                        .build())
                .addModifiers(Modifier.PUBLIC);

        ClassName entity = meta.getEntityType();

        builder.addMethod(MethodSpec.methodBuilder("patchMergeEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(BEAN_MAPPING)
                        .addMember("nullValuePropertyMappingStrategy", "$T.$L",
                                NULL_VALUE_PROPERTY_MAPPING_STRATEGY, "IGNORE")
                        .build())
                .addParameter(meta.getPatchType() != null ? meta.getPatchType() : meta.getEntityType(), "dto")
                .addParameter(ParameterSpec.builder(entity, "entity")
                        .addAnnotation(MAPPING_TARGET)
                        .build())
                .build());

        addPatchNestedMethods(builder, meta, elements);
        addOptionalMappingMethod(builder);

        return JavaFile.builder(meta.getGeneratedPackageName(), builder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    // ========================================================================
    //  嵌套 Patch 转换方法（含递归）
    // ========================================================================

    /**
     * 为 Patch Mapper 添加嵌套类型转换方法，并递归扫描所有嵌套层级。
     * <p>扫描 Patch DTO 中与 Entity 类型不一致的字段，为每对类型生成
     * 带有 {@code @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)} 的转换方法，
     * 确保嵌套对象的 PATCH 语义正确。</p>
     * <p>递归过程通过 {@code visitedPairs} 检测循环引用，防止无限递归。</p>
     */
    private static void addPatchNestedMethods(TypeSpec.Builder builder, RepositoryMeta meta, Elements elements) {
        if (meta.getPatchType() == null) return;

        Set<String> added = new HashSet<>();
        Set<String> visitedPairs = new HashSet<>();

        // 构建实体字段索引
        Map<String, SchemaFieldMeta> entityFields = new HashMap<>();
        for (SchemaFieldMeta ef : meta.getEntityAllFields()) {
            entityFields.put(ef.getFieldName(), ef);
        }

        for (SchemaFieldMeta dtoField : meta.getPatchTypeFields()) {
            SchemaFieldMeta entityField = entityFields.get(dtoField.getFieldName());
            if (entityField == null) continue;

            String dtoType = dtoField.getTypeSimpleName();
            String entityType = entityField.getTypeSimpleName();

            // 类型相同或为标量，无需嵌套转换
            if (dtoType.equals(entityType) || isScalarSimpleName(dtoType)) continue;

            // 生成 patch 方法并递归扫描嵌套
            addPatchHelperMethod(builder, dtoField, entityField, added);
            recursePatchNestedTypes(builder, elements,
                    dtoField.getTypeQualifiedName(),
                    entityField.getTypeQualifiedName(),
                    added, visitedPairs);
        }
    }

    /**
     * 生成单个 Patch 嵌套类型转换方法。
     */
    private static void addPatchHelperMethod(
            TypeSpec.Builder builder,
            SchemaFieldMeta dtoField,
            SchemaFieldMeta entityField,
            Set<String> added) {

        String methodName = "patch" + entityField.getTypeSimpleName();
        String key = methodName + "(" + dtoField.getTypeQualifiedName() + "," + entityField.getTypeQualifiedName() + ")";

        if (!added.add(key)) return;

        builder.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addAnnotation(AnnotationSpec.builder(BEAN_MAPPING)
                        .addMember("nullValuePropertyMappingStrategy", "$T.$L",
                                NULL_VALUE_PROPERTY_MAPPING_STRATEGY, "IGNORE")
                        .build())
                .addParameter(ClassName.bestGuess(dtoField.getTypeQualifiedName()), "source")
                .addParameter(ParameterSpec.builder(
                                ClassName.bestGuess(entityField.getTypeQualifiedName()), "target")
                        .addAnnotation(MAPPING_TARGET)
                        .build())
                .build());
    }

    /**
     * 递归扫描 Patch DTO → Entity 方向的嵌套类型，为所有深层的类型不匹配
     * 生成带 {@code @BeanMapping} 的 patch 方法。
     * <p>对于 {@code List<A> ↔ List<B>} 形式，提取泛型类型参数后递归处理。</p>
     */
    private static void recursePatchNestedTypes(
            TypeSpec.Builder builder, Elements elements,
            String dtoTypeQName, String entityTypeQName,
            Set<String> added, Set<String> visitedPairs) {

        if (elements == null) return;

        String pairKey = dtoTypeQName + "→" + entityTypeQName + ":PATCH";
        if (!visitedPairs.add(pairKey)) return;

        // 若任一类型不可内省，跳过
        if (isNonIntrospectable(dtoTypeQName, elements)
                || isNonIntrospectable(entityTypeQName, elements)) {
            return;
        }

        String dtoRaw = extractRawType(dtoTypeQName);
        String entityRaw = extractRawType(entityTypeQName);

        if (dtoRaw.equals(entityRaw)) {
            // 相同 raw type → 处理泛型参数
            List<String> dtoParams = extractTypeParameters(dtoTypeQName);
            List<String> entityParams = extractTypeParameters(entityTypeQName);
            if (!dtoParams.isEmpty() && dtoParams.size() == entityParams.size()) {
                for (int i = 0; i < dtoParams.size(); i++) {
                    if (!dtoParams.get(i).equals(entityParams.get(i))) {
                        recursePatchNestedTypes(builder, elements,
                                dtoParams.get(i), entityParams.get(i),
                                added, visitedPairs);
                    }
                }
            }
            return;
        }

        // 不同 raw type → 按字段名匹配扫描
        TypeElement dtoEl = elements.getTypeElement(dtoRaw);
        TypeElement entityEl = elements.getTypeElement(entityRaw);
        if (dtoEl == null || entityEl == null) return;

        Map<String, String> dtoFields = collectFields(dtoEl, elements);
        Map<String, String> entityFields = collectFields(entityEl, elements);

        for (Map.Entry<String, String> entry : dtoFields.entrySet()) {
            String fieldName = entry.getKey();
            String dtoFieldType = entry.getValue();
            String entityFieldType = entityFields.get(fieldName);
            if (entityFieldType == null || dtoFieldType.equals(entityFieldType)) continue;

            String dtoSimple = simpleName(dtoFieldType);
            String entitySimple = simpleName(entityFieldType);
            if (dtoSimple.equals(entitySimple) || isScalarSimpleName(dtoSimple)) continue;

            // 生成 patch 方法
            String methodName = "patch" + entitySimple;
            String methodKey = methodName + "(" + dtoFieldType + "," + entityFieldType + ")";

            if (added.add(methodKey)) {
                builder.addMethod(MethodSpec.methodBuilder(methodName)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addAnnotation(AnnotationSpec.builder(BEAN_MAPPING)
                                .addMember("nullValuePropertyMappingStrategy", "$T.$L",
                                        NULL_VALUE_PROPERTY_MAPPING_STRATEGY, "IGNORE")
                                .build())
                        .addParameter(ClassName.bestGuess(dtoFieldType), "source")
                        .addParameter(ParameterSpec.builder(
                                        ClassName.bestGuess(entityFieldType), "target")
                                .addAnnotation(MAPPING_TARGET)
                                .build())
                        .build());
            }

            // 递归
            recursePatchNestedTypes(builder, elements, dtoFieldType, entityFieldType,
                    added, visitedPairs);
        }
    }

    // ========================================================================
    //  Optional 映射
    // ========================================================================

    /**
     * 为 Mapper 添加 {@code Optional<T> → T} 映射方法。
     * <p>MapStruct 在遇到源类型为 {@code Optional<T>} 时会调用此方法，
     * 提取包装值或将 null Optional 转换为 null。</p>
     */
    public static void addOptionalMappingMethod(TypeSpec.Builder builder) {
        TypeVariableName t = TypeVariableName.get("T");

        builder.addMethod(MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addTypeVariable(t)
                .returns(t)
                .addParameter(ParameterizedTypeName.get(OPTIONAL, t), "value")
                .addStatement("return value == null ? null : value.orElse(null)")
                .build());
    }
}
