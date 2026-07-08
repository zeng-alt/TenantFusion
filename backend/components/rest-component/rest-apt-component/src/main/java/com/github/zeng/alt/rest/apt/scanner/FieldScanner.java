package com.github.zeng.alt.rest.apt.scanner;

import com.github.zeng.alt.rest.annotation.CrudRest;
import com.github.zeng.alt.rest.annotation.QueryField;
import com.github.zeng.alt.rest.annotation.QueryOrder;
import com.github.zeng.alt.rest.annotation.QueryRange;
import com.github.zeng.alt.rest.annotation.QueryType;
import com.github.zeng.alt.rest.apt.meta.MethodMeta;
import com.github.zeng.alt.rest.apt.meta.QueryFieldMeta;
import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.github.zeng.alt.rest.apt.meta.SchemaFieldMeta;
import com.squareup.javapoet.ClassName;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 字段扫描器 — 从 {@link TypeElement} 中提取查询字段和实体字段元数据。
 * <p>职责单一：封装 APT 环境下的字段读取、注解解析、类型提取逻辑。</p>
 *
 * @author zengJiaJun
 * @since 2026年07月07日
 * @version 1.0
 */
public class FieldScanner {

    private final Types typeUtils;
    private final Elements elementUtils;
    private final javax.annotation.processing.Messager messager;

    public FieldScanner(Types typeUtils, Elements elementUtils,
                        javax.annotation.processing.Messager messager) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
        this.messager = messager;
    }

    // ========================================================================
    //  查询字段扫描
    // ========================================================================

    /**
     * 解析 {@code queryType}（默认使用 {@code entityTypeMirror}），
     * 扫描其字段上的 {@code @QueryField / @QueryRange / @QueryOrder} 注解。
     */
    public void parseQueryType(RepositoryMeta.Builder metaBuilder, CrudRest annotation,
                               TypeMirror entityTypeMirror) {
        TypeMirror queryTypeMirror = resolveTypeMirror(() -> annotation.queryType());
        if (queryTypeMirror == null || isVoidType(queryTypeMirror)) {
            queryTypeMirror = entityTypeMirror;
        }

        Element queryTypeElement = typeUtils.asElement(queryTypeMirror);
        if (!(queryTypeElement instanceof TypeElement queryTypeEl)) {
            return;
        }

        ClassName queryType = ClassName.get(queryTypeEl);
        metaBuilder.queryType(queryType);

        scanQueryFields(queryTypeEl, metaBuilder);

        messager.printMessage(Diagnostic.Kind.NOTE,
                "解析 queryType: " + queryType
                        + " -> " + metaBuilder.build().getQueryFields().size() + " 个查询字段");
    }

    /**
     * 递归扫描类型及其父类的字段上的 @QueryField / @QueryRange / @QueryOrder 注解。
     */
    private void scanQueryFields(TypeElement typeElement, RepositoryMeta.Builder metaBuilder) {
        TypeMirror superclass = typeElement.getSuperclass();
        if (!isObjectType(superclass)) {
            Element superElement = typeUtils.asElement(superclass);
            if (superElement instanceof TypeElement superTypeEl) {
                scanQueryFields(superTypeEl, metaBuilder);
            }
        }

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) {
                continue;
            }

            String fieldName = enclosed.getSimpleName().toString();
            TypeMirror fieldTypeMirror = enclosed.asType();
            String typeQualifiedName = fieldTypeMirror.toString();
            String typeSimpleName = extractSimpleName(fieldTypeMirror);

            QueryFieldMeta.Builder fieldBuilder = QueryFieldMeta.builder()
                    .fieldName(fieldName)
                    .typeQualifiedName(typeQualifiedName)
                    .typeSimpleName(typeSimpleName);

            QueryField queryField = enclosed.getAnnotation(QueryField.class);
            if (queryField != null) {
                fieldBuilder.queryType(queryField.type())
                        .column(queryField.column())
                        .ignoreNull(queryField.ignoreNull())
                        .multi(queryField.multi());
            }

            QueryRange queryRange = enclosed.getAnnotation(QueryRange.class);
            if (queryRange != null) {
                fieldBuilder.rangeStart(queryRange.start())
                        .rangeEnd(queryRange.end());
                if (queryField == null) {
                    fieldBuilder.queryType(QueryType.BETWEEN);
                }
            }

            QueryOrder queryOrder = enclosed.getAnnotation(QueryOrder.class);
            if (queryOrder != null) {
                fieldBuilder.hasOrder(true)
                        .orderAsc(queryOrder.asc())
                        .orderPriority(queryOrder.order())
                        .autoSort(queryOrder.autoSort());
            }

            fieldBuilder.description(extractSchemaDescription(enclosed));

            if (queryField != null || queryRange != null || queryOrder != null) {
                metaBuilder.addQueryField(fieldBuilder.build());
            }
        }
    }

    // ========================================================================
    //  实体字段扫描（用于 OpenAPI Schema）
    // ========================================================================

    /**
     * 递归扫描实体及其父类的所有非静态字段（含 {@code @JsonIgnore}、不含 {@code @Transient}），
     * 用于 DTO 转换时的字段名匹配。
     */
    public void scanEntityFieldsAll(TypeElement typeElement, Consumer<SchemaFieldMeta> collector) {
        scanEntityFields(typeElement, collector, false);
    }

    /**
     * 递归扫描实体及其父类的所有非静态、非 {@code @JsonIgnore}、非 {@code @Transient} 字段。
     *
     * @param typeElement 待扫描的实体或 DTO 类型
     * @param collector   收集 SchemaFieldMeta 的 Consumer
     */
    public void scanEntityFields(TypeElement typeElement, Consumer<SchemaFieldMeta> collector) {
        scanEntityFields(typeElement, collector, true);
    }

    private void scanEntityFields(TypeElement typeElement, Consumer<SchemaFieldMeta> collector,
                                  boolean skipJsonIgnore) {
        TypeMirror superclass = typeElement.getSuperclass();
        if (!isObjectType(superclass)) {
            Element superElement = typeUtils.asElement(superclass);
            if (superElement instanceof TypeElement superTypeEl) {
                scanEntityFields(superTypeEl, collector, skipJsonIgnore);
            }
        }

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) {
                continue;
            }
            if (enclosed.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            if (hasAnnotation(enclosed, "jakarta.persistence.Transient")) {
                continue;
            }
            if (skipJsonIgnore && hasAnnotation(enclosed, "com.fasterxml.jackson.annotation.JsonIgnore")) {
                continue;
            }

            String fieldName = enclosed.getSimpleName().toString();
            TypeMirror fieldTypeMirror = enclosed.asType();
            String typeQualifiedName = fieldTypeMirror.toString();
            String typeSimpleName = extractSimpleName(fieldTypeMirror);
            String description = extractSchemaDescription(enclosed);

            collector.accept(SchemaFieldMeta.of(
                    fieldName, typeQualifiedName, typeSimpleName, description));
        }
    }

    // ========================================================================
    //  TypeMirror 工具
    // ========================================================================

    /**
     * 从 TypeMirror 中提取类型的 simple name
     */
    private String extractSimpleName(TypeMirror typeMirror) {
        Element fieldTypeElement = typeUtils.asElement(typeMirror);
        if (fieldTypeElement instanceof TypeElement fieldTypeEl) {
            return fieldTypeEl.getSimpleName().toString();
        }
        return "";
    }

    private static boolean isObjectType(TypeMirror tm) {
        return "java.lang.Object".equals(tm.toString());
    }

    private static boolean isVoidType(TypeMirror tm) {
        String name = tm.toString();
        return "java.lang.Void".equals(name) || "void".equals(name);
    }

    // ========================================================================
    //  注解工具
    // ========================================================================

    private static boolean hasAnnotation(Element element, String annotationQualifiedName) {
        for (AnnotationMirror am : element.getAnnotationMirrors()) {
            if (am.getAnnotationType().toString().equals(annotationQualifiedName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从字段的 {@code @Schema} 注解中提取描述信息，优先取 description，其次取 name。
     */
    private static String extractSchemaDescription(Element fieldElement) {
        for (AnnotationMirror am : fieldElement.getAnnotationMirrors()) {
            if (!"io.swagger.v3.oas.annotations.media.Schema".equals(
                    am.getAnnotationType().toString())) {
                continue;
            }
            String description = "";
            String name = "";
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                    am.getElementValues().entrySet()) {
                String key = entry.getKey().getSimpleName().toString();
                Object value = entry.getValue().getValue();
                if ("description".equals(key) && value instanceof String s && !s.isEmpty()) {
                    description = s;
                }
                if ("name".equals(key) && value instanceof String s && !s.isEmpty()) {
                    name = s;
                }
            }
            return !description.isEmpty() ? description : name;
        }
        return "";
    }

    // ========================================================================
    //  @CrudRest 操作类型解析（createType / updateType / detailType / listType）
    // ========================================================================

    /**
     * 解析单个操作类型的 ClassName，返回 null 表示未设置（void）
     */
    public ClassName parseOperationType(Supplier<Class<?>> supplier) {
        TypeMirror tm = resolveTypeMirror(supplier);
        if (tm == null || isVoidType(tm)) {
            return null;
        }
        Element el = typeUtils.asElement(tm);
        if (el instanceof TypeElement typeEl) {
            return ClassName.get(typeEl);
        }
        return null;
    }

    /**
     * 安全获取 Class 类型注解属性的 TypeMirror，自动处理 MirroredTypeException。
     */
    private TypeMirror resolveTypeMirror(Supplier<Class<?>> supplier) {
        try {
            Class<?> clazz = supplier.get();
            TypeElement te = elementUtils.getTypeElement(clazz.getCanonicalName());
            if (te != null) {
                return te.asType();
            }
            return null;
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }
}
