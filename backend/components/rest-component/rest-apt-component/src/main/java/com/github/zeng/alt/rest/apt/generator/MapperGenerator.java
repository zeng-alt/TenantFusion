package com.github.zeng.alt.rest.apt.generator;

import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.github.zeng.alt.rest.apt.meta.SchemaFieldMeta;
import com.squareup.javapoet.*;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.*;

import static com.github.zeng.alt.rest.apt.generator.PatchMapperGenerator.addOptionalMappingMethod;
import static com.github.zeng.alt.rest.apt.generator.GeneratorUtils.*;

/**
 * Mapper 生成器 — 当项目引入 MapStruct 时，生成 MapStruct Mapper 接口。
 * <p>生成的 Mapper 会被 {@link HandlerGenerator} 引用，替代原来的
 * {@code BeanUtils.copyProperties} 调用。</p>
 *
 * @author zengJiaJun
 * @since 2026年07月07日
 * @version 1.0
 */
public final class MapperGenerator {

    private static final ClassName MAPPER = ClassName.get("org.mapstruct", "Mapper");
    private static final ClassName MAPPING_TARGET = ClassName.get("org.mapstruct", "MappingTarget");
    private static final ClassName REPORTING_POLICY = ClassName.get("org.mapstruct", "ReportingPolicy");

    private MapperGenerator() {}

    /**
     * 为指定 Repository 元模型生成 MapStruct Mapper 接口。
     *
     * @param meta  Repository 元模型
     * @param elements APT Elements 工具，用于递归解析嵌套类型字段；可 null（降级为单层转换）
     * @return JavaFile 表示生成的 Mapper 接口
     */
    public static JavaFile generate(RepositoryMeta meta, Elements elements) {
        String mapperName = meta.getEntitySimpleName() + "Mapper";

        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(mapperName)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", "rest-apt-component")
                        .build())
                .addAnnotation(AnnotationSpec.builder(MAPPER)
                        .addMember("componentModel", "$S", "spring")
                        .addMember("unmappedTargetPolicy", "$T.$L", REPORTING_POLICY, "IGNORE")
                        .build())
                .addModifiers(Modifier.PUBLIC);

        ClassName entity = meta.getEntityType();

        // Entity -> ListDTO
        if (meta.getListType() != null && !meta.getListType().equals(entity)) {
            builder.addMethod(MethodSpec.methodBuilder("toListDto")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(meta.getListType())
                    .addParameter(entity, "entity")
                    .build());
        }

        // Entity -> DetailDTO
        if (meta.getDetailType() != null && !meta.getDetailType().equals(entity)) {
            builder.addMethod(MethodSpec.methodBuilder("toDetailDto")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(meta.getDetailType())
                    .addParameter(entity, "entity")
                    .build());
        }

        // Create DTO -> Entity
        if (meta.getCreateType() != null && !meta.getCreateType().equals(entity)) {
            builder.addMethod(MethodSpec.methodBuilder("toEntity")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(entity)
                    .addParameter(meta.getCreateType(), "dto")
                    .build());
        }

        // Update DTO -> Entity
        if (meta.getUpdateType() != null && !meta.getUpdateType().equals(entity)) {
            builder.addMethod(MethodSpec.methodBuilder("mergeEntity")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(meta.getUpdateType(), "dto")
                    .addParameter(ParameterSpec.builder(entity, "entity")
                            .addAnnotation(MAPPING_TARGET)
                            .build())
                    .build());
        }

        // Search DTO -> Entity
        if (meta.getSearchType() != null && !meta.getSearchType().equals(entity)) {
            builder.addMethod(MethodSpec.methodBuilder("toSearchDto")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(meta.getSearchType())
                    .addParameter(entity, "entity")
                    .build());
        }

        addNestedConversionMethods(builder, meta, elements);
        addOptionalMappingMethod(builder);

        return JavaFile.builder(meta.getGeneratedPackageName(), builder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    // ========================================================================
    //  嵌套类型转换方法生成（含递归）
    // ========================================================================

    /**
     * 为同名字段但类型不同的实体↔DTO 转换添加辅助方法，并递归扫描所有嵌套层级。
     * <p>MapStruct 在遇到字段类型不匹配时，会查找同一 Mapper 中是否有可用的
     * 方法来完成转换。此处生成的方法供主转换方法（如 toListDto）自动调用。</p>
     * <p>递归过程通过 {@code visitedPairs} 检测循环引用（如 User↔Address 双向引用），
     * 确保不会无限递归。</p>
     */
    private static void addNestedConversionMethods(TypeSpec.Builder builder, RepositoryMeta meta, Elements elements) {
        ClassName entityType = meta.getEntityType();

        // 构建实体字段名 → SchemaFieldMeta 索引（供根层级使用）
        Map<String, SchemaFieldMeta> entityFieldMap = new HashMap<>();
        for (SchemaFieldMeta ef : meta.getEntityAllFields()) {
            entityFieldMap.put(ef.getFieldName(), ef);
        }

        Set<String> addedMethods = new LinkedHashSet<>();
        Set<String> visitedPairs = new HashSet<>();

        // Entity → Dto 方向（list / detail）
        addConversionDirection(builder, entityFieldMap, addedMethods, visitedPairs,
                meta.getListType(), meta.getListTypeFields(), entityType, true, elements);
        addConversionDirection(builder, entityFieldMap, addedMethods, visitedPairs,
                meta.getDetailType(), meta.getDetailTypeFields(), entityType, true, elements);

        // Dto → Entity 方向（create / update / patch）
        addConversionDirection(builder, entityFieldMap, addedMethods, visitedPairs,
                meta.getCreateType(), meta.getCreateTypeFields(), entityType, false, elements);
        addConversionDirection(builder, entityFieldMap, addedMethods, visitedPairs,
                meta.getUpdateType(), meta.getUpdateTypeFields(), entityType, false, elements);
        addConversionDirection(builder, entityFieldMap, addedMethods, visitedPairs,
                meta.getPatchType(), meta.getPatchTypeFields(), entityType, false, elements);
    }

    /**
     * 处理一个 DTO ↔ Entity 方向的所有字段，为类型不匹配的嵌套对象生成转换方法，
     * 并递归扫描更深层级的嵌套类型。
     *
     * @param entityToDto true = Entity → DTO, false = DTO → Entity
     */
    private static void addConversionDirection(
            TypeSpec.Builder builder,
            Map<String, SchemaFieldMeta> entityFieldMap,
            Set<String> addedMethods,
            Set<String> visitedPairs,
            TypeName dtoType, List<SchemaFieldMeta> dtoFields,
            TypeName entityType, boolean entityToDto,
            Elements elements) {

        if (dtoType == null || dtoType.equals(entityType)) return;

        for (SchemaFieldMeta dtoField : dtoFields) {
            SchemaFieldMeta entityField = entityFieldMap.get(dtoField.getFieldName());
            if (entityField == null) continue;

            String dtoTypeSimple = dtoField.getTypeSimpleName();
            String entityTypeSimple = entityField.getTypeSimpleName();

            // 类型相同 → BeanUtils 已能处理，无需额外方法
            if (entityTypeSimple.equals(dtoTypeSimple)) continue;
            // 标量类型 → 无需嵌套转换
            if (isScalarSimpleName(entityToDto ? dtoTypeSimple : entityTypeSimple)) continue;

            if (entityToDto) {
                addEntityToDtoHelperMethod(builder, dtoField, entityField, addedMethods);
                recurseNestedTypes(builder, elements,
                        entityField.getTypeQualifiedName(),
                        dtoField.getTypeQualifiedName(),
                        true, addedMethods, visitedPairs);
            } else {
                addDtoToEntityHelperMethod(builder, dtoField, entityField, addedMethods);
                recurseNestedTypes(builder, elements,
                        dtoField.getTypeQualifiedName(),
                        entityField.getTypeQualifiedName(),
                        false, addedMethods, visitedPairs);
            }
        }
    }

    /**
     * 递归扫描 sourceType → targetType 的类型字段，为所有嵌套的类型不匹配生成 conversion 方法。
     * <p>同时处理普通对象类型和泛型类型参数（如 {@code List<Address> ↔ List<AddressDto>}）。</p>
     *
     * @param visitedPairs  "sourceType→targetType:entityToDto" 字符串集合，用于循环检测
     */
    private static void recurseNestedTypes(
            TypeSpec.Builder builder, Elements elements,
            String sourceTypeQName, String targetTypeQName,
            boolean entityToDto,
            Set<String> addedMethods, Set<String> visitedPairs) {

        if (elements == null) return;

        String pairKey = sourceTypeQName + "→" + targetTypeQName + ":" + entityToDto;
        if (!visitedPairs.add(pairKey)) return;

        // 若 source 或 target 不可内省（标量、标准库、无法解析），跳过
        if (isNonIntrospectable(sourceTypeQName, elements)
                || isNonIntrospectable(targetTypeQName, elements)) {
            return;
        }

        String srcRaw = extractRawType(sourceTypeQName);
        String tgtRaw = extractRawType(targetTypeQName);

        if (srcRaw.equals(tgtRaw)) {
            // 相同 raw type → 比较泛型类型参数（如 List<A> ↔ List<B>, Optional<A> ↔ Optional<B>）
            recurseTypeParameters(builder, elements, sourceTypeQName, targetTypeQName,
                    entityToDto, addedMethods, visitedPairs);
            return;
        }

        // 不同 raw type → 按字段名匹配扫描嵌套字段
        TypeElement sourceEl = elements.getTypeElement(srcRaw);
        TypeElement targetEl = elements.getTypeElement(tgtRaw);
        if (sourceEl == null || targetEl == null) return;

        Map<String, String> srcFields = collectFields(sourceEl, elements);
        Map<String, String> tgtFields = collectFields(targetEl, elements);

        for (Map.Entry<String, String> entry : srcFields.entrySet()) {
            String fieldName = entry.getKey();
            String srcFieldType = entry.getValue();
            String tgtFieldType = tgtFields.get(fieldName);
            if (tgtFieldType == null || srcFieldType.equals(tgtFieldType)) continue;

            String srcSimple = simpleName(srcFieldType);
            String tgtSimple = simpleName(tgtFieldType);
            if (srcSimple.equals(tgtSimple)) continue;
            if (isScalarSimpleName(entityToDto ? tgtSimple : srcSimple)) continue;

            // 生成转换方法并递归
            String methodName = "to" + (entityToDto ? tgtSimple : srcSimple);
            String methodKey = methodName + "(" + (entityToDto ? srcFieldType : tgtFieldType) + ")";

            if (addedMethods.add(methodKey)) {
                builder.addMethod(MethodSpec.methodBuilder(methodName)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.bestGuess(tgtFieldType))
                        .addParameter(ClassName.bestGuess(srcFieldType), "source")
                        .build());
            }

            // 递归处理当前字段的嵌套类型
            recurseNestedTypes(builder, elements, srcFieldType, tgtFieldType,
                    entityToDto, addedMethods, visitedPairs);
        }
    }

    /**
     * 当两个类型具有相同的 raw type 但泛型参数不同时（如 {@code List<A> ↔ List<B>}），
     * 递归处理对应的泛型类型参数。
     */
    private static void recurseTypeParameters(
            TypeSpec.Builder builder, Elements elements,
            String sourceTypeQName, String targetTypeQName,
            boolean entityToDto,
            Set<String> addedMethods, Set<String> visitedPairs) {

        List<String> srcParams = extractTypeParameters(sourceTypeQName);
        List<String> tgtParams = extractTypeParameters(targetTypeQName);
        if (srcParams.isEmpty() || srcParams.size() != tgtParams.size()) return;

        for (int i = 0; i < srcParams.size(); i++) {
            String srcParam = srcParams.get(i);
            String tgtParam = tgtParams.get(i);
            if (!srcParam.equals(tgtParam)) {
                recurseNestedTypes(builder, elements, srcParam, tgtParam,
                        entityToDto, addedMethods, visitedPairs);
            }
        }
    }

    // ========================================================================
    //  单层方法生成
    // ========================================================================

    /**
     * Entity → Dto 嵌套类型转换方法。
     */
    private static void addEntityToDtoHelperMethod(
            TypeSpec.Builder builder,
            SchemaFieldMeta dtoField,
            SchemaFieldMeta entityField,
            Set<String> addedMethods) {

        String dtoTypeSimple = dtoField.getTypeSimpleName();
        String methodName = "to" + dtoTypeSimple;
        String key = methodName + "(" + entityField.getTypeQualifiedName() + ")";

        if (!addedMethods.contains(key)) {
            addedMethods.add(key);
            builder.addMethod(MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.bestGuess(dtoField.getTypeQualifiedName()))
                    .addParameter(ClassName.bestGuess(entityField.getTypeQualifiedName()), "source")
                    .build());
        }
    }

    /**
     * Dto → Entity 嵌套类型转换方法。
     */
    private static void addDtoToEntityHelperMethod(
            TypeSpec.Builder builder,
            SchemaFieldMeta dtoField,
            SchemaFieldMeta entityField,
            Set<String> addedMethods) {

        String entityTypeSimple = entityField.getTypeSimpleName();
        String methodName = "to" + entityTypeSimple;
        String key = methodName + "(" + dtoField.getTypeQualifiedName() + ")";

        if (addedMethods.add(key)) {
            builder.addMethod(MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.bestGuess(entityField.getTypeQualifiedName()))
                    .addParameter(ClassName.bestGuess(dtoField.getTypeQualifiedName()), "source")
                    .build());
        }
    }
}
