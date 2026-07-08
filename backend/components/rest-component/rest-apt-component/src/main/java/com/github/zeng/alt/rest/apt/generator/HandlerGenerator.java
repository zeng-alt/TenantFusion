package com.github.zeng.alt.rest.apt.generator;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.rest.apt.meta.MethodMeta;
import com.github.zeng.alt.rest.apt.meta.QueryFieldMeta;
import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.github.zeng.alt.rest.apt.meta.SchemaFieldMeta;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import io.vavr.control.Option;
import jakarta.servlet.ServletException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.zeng.alt.rest.apt.generator.GeneratorUtils.*;

/**
 * Handler 生成器 — 使用 JavaPoet 生成 Handler 类
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 2.0
 */
public final class HandlerGenerator {

    private static final ClassName SERVER_REQUEST = ClassName.get(ServerRequest.class);
    private static final ClassName SERVER_RESPONSE = ClassName.get(ServerResponse.class);
    private static final ClassName REST_RESPONSE = ClassName.get(RestResponse.class);
    private static final ClassName PAGE_REST_RESPONSE = ClassName.get(PageRestResponse.class);
    private static final ClassName BOOLEAN_BUILDER = ClassName.get("com.querydsl.core", "BooleanBuilder");
    private static final ClassName PREDICATE = ClassName.get("com.querydsl.core.types", "Predicate");
    private static final ClassName SORT = ClassName.get("org.springframework.data.domain", "Sort");
    private static final ClassName SORT_DIRECTION = ClassName.get("org.springframework.data.domain.Sort", "Direction");
    private static final ClassName BEAN_UTILS = ClassName.get("org.springframework.beans", "BeanUtils");
    private static final ClassName TRANSACTIONAL = ClassName.get("org.springframework.transaction.annotation", "Transactional");


    private HandlerGenerator() {}

    /**
     * 生成 Handler 类（不使用 MapStruct，无 Elements => 不递归）
     */
    public static JavaFile generate(RepositoryMeta meta) {
        return generate(meta, false, null);
    }

    /**
     * 生成 Handler 类
     *
     * @param meta         Repository 元模型
     * @param useMapStruct 是否使用 MapStruct Mapper（为 true 时将生成 Mapper 注入和调用）
     * @param elements     APT Elements 工具，用于递归解析嵌套类型字段；可 null（降级为单层转换）
     */
    public static JavaFile generate(RepositoryMeta meta, boolean useMapStruct, Elements elements) {
        TypeSpec.Builder handlerBuilder = TypeSpec.classBuilder(meta.getHandlerSimpleName())
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", "rest-apt-component")
                        .build())
                .addAnnotation(Component.class)
                .addModifiers(Modifier.PUBLIC)
                .addField(ClassName.bestGuess(meta.getRepositorySimpleName()), "repository",
                        Modifier.PRIVATE, Modifier.FINAL);

        // 使用 MapStruct 时添加 Mapper 字段
        if (useMapStruct) {
            handlerBuilder.addField(getMapperTypeName(meta), getMapperFieldName(meta),
                    Modifier.PRIVATE, Modifier.FINAL);
            handlerBuilder.addField(getPatchMapperTypeName(meta), getPatchMapperFieldName(meta),
                    Modifier.PRIVATE, Modifier.FINAL);
        }

        // 构造器
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.bestGuess(meta.getRepositorySimpleName()), "repository")
                .addStatement("this.repository = repository");

        if (useMapStruct) {
            constructorBuilder.addParameter(getMapperTypeName(meta), getMapperFieldName(meta))
                    .addStatement("this.$L = $L", getMapperFieldName(meta), getMapperFieldName(meta));

            constructorBuilder.addParameter(getPatchMapperTypeName(meta), getPatchMapperFieldName(meta))
                    .addStatement("this.$L = $L", getPatchMapperFieldName(meta), getPatchMapperFieldName(meta));

        }

        handlerBuilder.addMethod(constructorBuilder.build());

        for (MethodMeta method : meta.getEnabledMethods()) {
            handlerBuilder.addMethod(buildMethod(meta, method, useMapStruct, elements));
        }

        // 如果存在 @QueryField 或 @QueryRange，生成 predicate 构建方法
        if (meta.isHasQueryFields()) {
            handlerBuilder.addMethod(buildPredicateMethod(meta));
        }

        // 如果存在 @QueryOrder，生成 sort 构建方法
        if (hasOrderFields(meta)) {
            handlerBuilder.addMethod(buildSortMethod(meta));
        }

        return JavaFile.builder(meta.getGeneratedPackageName(), handlerBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    private static boolean hasOrderFields(RepositoryMeta meta) {
        return meta.getQueryFields().stream().anyMatch(QueryFieldMeta::isHasOrder);
    }

    // ========================================================================
    //  Mapper 工具方法
    // ========================================================================

    /**
     * 获取 MapStruct Mapper 的 ClassName（与 Handler 同包）
     */
    private static ClassName getMapperTypeName(RepositoryMeta meta) {
        return ClassName.get(meta.getGeneratedPackageName(), meta.getEntitySimpleName() + "Mapper");
    }

    private static ClassName getPatchMapperTypeName(RepositoryMeta meta) {
        return ClassName.get(meta.getGeneratedPackageName(), meta.getEntitySimpleName() + "PatchMapper");
    }

    /**
     * 获取 MapStruct Mapper 的字段名（首字母小写驼峰）
     */
    private static String getMapperFieldName(RepositoryMeta meta) {
        String name = meta.getEntitySimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1) + "Mapper";
    }

    private static String getPatchMapperFieldName(RepositoryMeta meta) {
        String name = meta.getEntitySimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1) + "PatchMapper";
    }

    // ========================================================================
    //  方法构建
    // ========================================================================

    private static MethodSpec buildMethod(RepositoryMeta meta, MethodMeta method, boolean useMapStruct, Elements elements) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(SERVER_RESPONSE)
                .addParameter(ParameterSpec.builder(SERVER_REQUEST, "request").build());

        switch (method) {
            case LIST -> buildListMethod(meta, methodBuilder, useMapStruct, elements);
            case DETAIL -> buildDetailMethod(meta, methodBuilder, useMapStruct, elements);
            case CREATE -> buildCreateMethod(meta, methodBuilder, useMapStruct, elements);
            case UPDATE -> buildUpdateMethod(meta, methodBuilder, useMapStruct, elements);
            case PATCH -> buildPatchMethod(meta, methodBuilder, useMapStruct, elements);
            case DELETE -> buildDeleteMethod(meta, methodBuilder);
            case SORT -> buildSortBatchMethod(meta, methodBuilder, useMapStruct);
        }

        return methodBuilder.build();
    }

    // ========================================================================
    //  LIST
    // ========================================================================

    private static void buildListMethod(RepositoryMeta meta, MethodSpec.Builder builder, boolean useMapStruct, Elements elements) {
        TypeName entityType = meta.getEntityType();
        ClassName listType = meta.getListType();

        // 确定列表项类型和分页响应类型
        TypeName itemType = listType != null ? listType : entityType;
        TypeName pageType = ParameterizedTypeName.get(ClassName.get(Page.class), entityType);
        TypeName pageRestType = ParameterizedTypeName.get(PAGE_REST_RESPONSE, itemType);

        builder.addStatement("int page = Integer.parseInt(request.param($S).orElse($S))", "page", "1")
                .addStatement("int size = Integer.parseInt(request.param($S).orElse($S))", "size", "10");

        boolean hasQueryFields = meta.isHasQueryFields();
        boolean hasSort = hasOrderFields(meta);

        if (hasQueryFields && hasSort) {
            builder.addStatement("$T pageResult = repository.findAll(buildPredicate(request), $T.of(page - 1, size, buildSort()))",
                    pageType, PageRequest.class);
        } else if (hasQueryFields) {
            builder.addStatement("$T pageResult = repository.findAll(buildPredicate(request), $T.of(page - 1, size))",
                    pageType, PageRequest.class);
        } else if (hasSort) {
            builder.addStatement("$T pageResult = repository.findAll($T.of(page - 1, size, buildSort()))",
                    pageType, PageRequest.class);
        } else {
            builder.addStatement("$T pageResult = repository.findAll($T.of(page - 1, size))",
                    pageType, PageRequest.class);
        }

        if (listType != null) {
            // entity → DTO 转换
            builder.addStatement("$T<$T> dtoList = new $T<>(pageResult.getContent().size())",
                            ClassName.get(List.class), listType, ClassName.get(ArrayList.class))
                    .beginControlFlow("for ($T entity : pageResult.getContent())", entityType);

            if (useMapStruct) {
                // 使用 MapStruct Mapper
                builder.addStatement("$T dto = $L.toListDto(entity)", listType, getMapperFieldName(meta));
            } else {
                // 使用 BeanUtils
                builder.addStatement("$T dto = new $T()", listType, listType)
                        .addStatement("$T.copyProperties(entity, dto)", BEAN_UTILS);
                // 嵌套 DTO 转换（递归处理所有层级）
                generateNestedDtoConversions(builder, meta, elements,
                        meta.getEntityAllFields(), meta.getListTypeFields(), "entity", "dto");
            }

            builder.addStatement("dtoList.add(dto)")
                    .endControlFlow()
                    .addStatement("$T response = $T.of(dtoList, pageResult.getTotalElements(), size, page)",
                            pageRestType, PAGE_REST_RESPONSE);
        } else {
            builder.addStatement("$T response = $T.of(pageResult.getContent(), pageResult.getTotalElements(), size, page)",
                    pageRestType, PAGE_REST_RESPONSE);
        }

        builder.addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body(response)",
                SERVER_RESPONSE, MediaType.class);
    }

    // ========================================================================
    //  DETAIL
    // ========================================================================

    private static void buildDetailMethod(RepositoryMeta meta, MethodSpec.Builder builder, boolean useMapStruct, Elements elements) {
        TypeName entityType = meta.getEntityType();
        TypeName idType = meta.getIdType();
        TypeName optionType = ParameterizedTypeName.get(ClassName.get(Option.class), entityType);
        ClassName detailType = meta.getDetailType();

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idType, ClassName.get(Long.class), "id")
                .addStatement("$T result = repository.findById(id)", optionType);

        if (detailType != null) {
            if (useMapStruct) {
                // 使用 MapStruct Mapper 转换
                builder.beginControlFlow("if (result.isDefined())")
                        .addStatement("$T value = result.get()", entityType)
                        .addStatement("$T dto = $L.toDetailDto(value)", detailType, getMapperFieldName(meta))
                        .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success(dto))",
                                SERVER_RESPONSE, MediaType.class, REST_RESPONSE)
                        .nextControlFlow("else")
                        .addStatement("return $T.notFound().build()", SERVER_RESPONSE)
                        .endControlFlow();
            } else {
                // entity → DTO 转换后返回
                builder.beginControlFlow("if (result.isDefined())")
                        .addStatement("$T value = result.get()", entityType)
                        .addStatement("$T dto = new $T()", detailType, detailType)
                        .addStatement("$T.copyProperties(value, dto)", BEAN_UTILS);
                generateNestedDtoConversions(builder, meta, elements, meta.getEntityAllFields(), meta.getDetailTypeFields(), "value", "dto");
                builder.addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success(dto))",
                                SERVER_RESPONSE, MediaType.class, REST_RESPONSE)
                        .nextControlFlow("else")
                        .addStatement("return $T.notFound().build()", SERVER_RESPONSE)
                        .endControlFlow();
            }
        } else {
            builder.addStatement("return result.map(value -> $T.ok().contentType($T.APPLICATION_JSON).body($T.success(value)))" +
                            ".getOrElse($T.notFound().build())",
                    SERVER_RESPONSE, MediaType.class, REST_RESPONSE, SERVER_RESPONSE);
        }
    }

    // ========================================================================
    //  CREATE
    // ========================================================================

    private static void buildCreateMethod(RepositoryMeta meta, MethodSpec.Builder builder, boolean useMapStruct, Elements elements) {
        TypeName entityType = meta.getEntityType();
        ClassName createType = meta.getCreateType();

        builder.addAnnotation(
                AnnotationSpec.builder(TRANSACTIONAL)
                        .addMember(
                                "rollbackFor",
                                "$T.class",
                                Exception.class)
                        .build());

        if (createType != null) {
            if (useMapStruct) {
                // 使用 MapStruct Mapper 转换 DTO → Entity
                builder.addStatement("$T dto = request.body($T.class)", createType, createType)
                        .addStatement("$T entity = $L.toEntity(dto)", entityType, getMapperFieldName(meta));
            } else {
                // DTO → entity 转换后保存
                builder.addStatement("$T dto = request.body($T.class)", createType, createType)
                        .addStatement("$T entity = new $T()", entityType, entityType)
                        .addStatement("$T.copyProperties(dto, entity)", BEAN_UTILS);
                generateReverseNestedDtoConversions(builder, meta, elements, meta.getCreateTypeFields(), meta.getEntityAllFields(), "dto", "entity");
            }
            generateAutoSortCode(builder, meta);
            builder.addStatement("$T saved = repository.save(entity)", entityType);
        } else {
            builder.addStatement("$T entity = request.body($T.class)", entityType, entityType);
            generateAutoSortCode(builder, meta);
            builder.addStatement("$T saved = repository.save(entity)", entityType);
        }

        builder.addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success(saved))",
                        SERVER_RESPONSE, MediaType.class, REST_RESPONSE)
                .addException(ServletException.class)
                .addException(IOException.class);
    }

    // ========================================================================
    //  AutoSort — 自动递增
    // ========================================================================

    /**
     * 如果实体中存在 {@code @QueryOrder(autoSort = true)} 的数字类型字段，
     * 在 CREATE 时自动查询当前最大值并 +1，实现排序字段自动递增。
     * <p>生成代码示例（Integer 类型）：</p>
     * <pre>{@code
     * Sort __autoSort = Sort.by(Sort.Direction.DESC, "order");
     * Page<Entity> __topPage = repository.findAll(PageRequest.of(0, 1, __autoSort));
     * Integer __maxVal = __topPage.hasContent() ? __topPage.getContent().get(0).getOrder() + 1 : 1;
     * entity.setOrder(__maxVal);
     * }</pre>
     */
    private static void generateAutoSortCode(MethodSpec.Builder builder, RepositoryMeta meta) {
        QueryFieldMeta autoSortField = meta.getQueryFields().stream()
                .filter(f -> f.isAutoSort() && f.isNumberType())
                .findFirst()
                .orElse(null);
        if (autoSortField == null) {
            return;
        }

        TypeName entityType = meta.getEntityType();
        String fieldName = autoSortField.getFieldName();
        String cap = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String typeQName = autoSortField.getTypeQualifiedName();
        TypeName fieldType = ClassName.bestGuess(typeQName);

        // Sort __autoSort = Sort.by(Sort.Direction.DESC, fieldName)
        builder.addStatement("$T __autoSort = $T.by($T.$L, $S)",
                SORT, SORT, SORT_DIRECTION, "DESC", fieldName);

        // Page<EntityType> __topPage = repository.findAll(PageRequest.of(0, 1, __autoSort))
        TypeName pageType = ParameterizedTypeName.get(ClassName.get(Page.class), entityType);
        builder.addStatement("$T __topPage = repository.findAll($T.of(0, 1, __autoSort))",
                pageType, PageRequest.class);

        switch (typeQName) {
            case "java.lang.Integer", "int":
                builder.addStatement("$T __maxVal = __topPage.hasContent() ? __topPage.getContent().get(0).get$L() + 1 : 1",
                        fieldType, cap);
                break;
            case "java.lang.Long", "long":
                builder.addStatement("$T __maxVal = __topPage.hasContent() ? __topPage.getContent().get(0).get$L() + 1L : 1L",
                        fieldType, cap);
                break;
            case "java.lang.Double", "double":
                builder.addStatement("$T __maxVal = __topPage.hasContent() ? __topPage.getContent().get(0).get$L() + 1.0 : 1.0",
                        fieldType, cap);
                break;
            case "java.math.BigDecimal":
                builder.addStatement("$T __maxVal = __topPage.hasContent() ? __topPage.getContent().get(0).get$L().add(java.math.BigDecimal.ONE) : java.math.BigDecimal.ONE",
                        fieldType, cap);
                break;
            default:
                return;
        }

        builder.addStatement("entity.set$L(__maxVal)", cap);
    }

    // ========================================================================
    //  UPDATE
    // ========================================================================

    private static void buildUpdateMethod(RepositoryMeta meta, MethodSpec.Builder builder, boolean useMapStruct, Elements elements) {
        TypeName entityType = meta.getEntityType();
        TypeName idType = meta.getIdType();
        TypeName optionType = ParameterizedTypeName.get(ClassName.get(Option.class), entityType);
        ClassName updateType = meta.getUpdateType();

        builder.addAnnotation(
                AnnotationSpec.builder(TRANSACTIONAL)
                        .addMember(
                                "rollbackFor",
                                "$T.class",
                                Exception.class)
                        .build());

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                idType, ClassName.get(Long.class), "id");

        if (updateType != null) {
            // DTO 模式：读取 DTO，合并到已有实体
            builder.addStatement("$T dto = request.body($T.class)", updateType, updateType)
                    .addStatement("$T result = repository.findById(id)", optionType)
                    .beginControlFlow("if (result.isEmpty())")
                    .addStatement("return $T.notFound().build()", SERVER_RESPONSE)
                    .endControlFlow()
                    .addStatement("$T entity = result.get()", entityType);

            if (useMapStruct) {
                // 使用 MapStruct Mapper 合并（@MappingTarget）
                builder.addStatement("$L.mergeEntity(dto, entity)", getMapperFieldName(meta));
            } else {
                builder.addStatement("$T.copyProperties(dto, entity)", BEAN_UTILS);
                generateReverseNestedDtoConversions(builder, meta, elements, meta.getUpdateTypeFields(), meta.getEntityAllFields(), "dto", "entity");
            }

            builder.addStatement("repository.save(entity)")
                    .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success(entity))",
                            SERVER_RESPONSE, MediaType.class, REST_RESPONSE);
        } else {
            // 实体模式：直接使用请求体实体覆盖保存
            builder.addStatement("$T entity = request.body($T.class)", entityType, entityType)
                    .addStatement("$T result = repository.findById(id)", optionType)
                    .beginControlFlow("if (result.isEmpty())")
                    .addStatement("return $T.notFound().build()", SERVER_RESPONSE)
                    .endControlFlow()
                    .addStatement("repository.save(entity)")
                    .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success(entity))",
                            SERVER_RESPONSE, MediaType.class, REST_RESPONSE);
        }

        builder.addException(ServletException.class)
                .addException(IOException.class);
    }

    // ========================================================================
    //  PATCH（部分更新，跳过 null 字段）
    // ========================================================================

    private static void buildPatchMethod(RepositoryMeta meta, MethodSpec.Builder builder, boolean useMapStruct, Elements elements) {
        TypeName entityType = meta.getEntityType();
        TypeName idType = meta.getIdType();
        TypeName optionType = ParameterizedTypeName.get(ClassName.get(Option.class), entityType);

        // patchType → updateType → entityType（三级 fallback）
        ClassName patchBodyType = meta.getPatchType() != null ? meta.getPatchType() : meta.getEntityType();

        builder.addAnnotation(
                AnnotationSpec.builder(TRANSACTIONAL)
                        .addMember(
                                "rollbackFor",
                                "$T.class",
                                Exception.class)
                        .build());

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idType, ClassName.get(Long.class), "id")
                .addStatement("$T dto = request.body($T.class)", patchBodyType, patchBodyType)
                .addStatement("$T result = repository.findById(id)", optionType)
                .beginControlFlow("if (result.isEmpty())")
                .addStatement("return $T.notFound().build()", SERVER_RESPONSE)
                .endControlFlow()
                .addStatement("$T entity = result.get()", entityType);

        if (useMapStruct) {
            // 使用 MapStruct Mapper 合并（@MappingTarget）
            builder.addStatement("$L.patchMergeEntity(dto, entity)", getPatchMapperFieldName(meta));
        } else {
            // 零反射 null-safe 复制 — 按字段逐个 if-not-null，原生编译安全
            // 选用字段列表：patchType 字段 → updateType 字段 → 实体所有字段
            List<SchemaFieldMeta> patchFields = !meta.getPatchTypeFields().isEmpty() ? meta.getPatchTypeFields()
                    : (!meta.getUpdateTypeFields().isEmpty() ? meta.getUpdateTypeFields()
                       : meta.getEntityAllFields());
            // 构建实体字段名 → 类型 索引
            Map<String, SchemaFieldMeta> entityFieldMap = new HashMap<>();
            for (SchemaFieldMeta ef : meta.getEntityAllFields()) {
                entityFieldMap.put(ef.getFieldName(), ef);
            }
            Set<String> patchSkipFields = Set.of(
                    "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate");

            // 分类：sameType（直接设值）、nestedObj（BeanUtils）、nestedColl（集合元素转换）
            Set<String> sameTypeFields = new HashSet<>();
            List<SchemaFieldMeta> nestedFields = new ArrayList<>();
            for (SchemaFieldMeta dtoField : patchFields) {
                if (patchSkipFields.contains(dtoField.getFieldName())) continue;
                String fieldName = dtoField.getFieldName();
                SchemaFieldMeta entityField = entityFieldMap.get(fieldName);
                if (entityField == null) continue;

                String dtoQName = dtoField.getTypeQualifiedName();
                String entityQName = entityField.getTypeQualifiedName();
                if (dtoQName.equals(entityQName)) {
                    // 全限定名相同 → 直接 null-safe 复制
                    sameTypeFields.add(fieldName);
                } else if (!isScalarSimpleName(dtoField.getTypeSimpleName())) {
                    // 不同类型且非标量 → 需要嵌套转换
                    nestedFields.add(dtoField);
                }
            }

            // 同类字段：null-safe 直接复制（走 setter）
            for (String fieldName : sameTypeFields) {
                String cap = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                builder.beginControlFlow("if (dto.$L() != null)", "get" + cap)
                        .addStatement("entity.$L(dto.$L())", "set" + cap, "get" + cap)
                        .endControlFlow();
            }

            // 不同类型嵌套字段：递归处理（含集合元素转换）
            Set<String> generatedFields = new HashSet<>();
            for (SchemaFieldMeta dtoField : nestedFields) {
                String fieldName = dtoField.getFieldName();
                SchemaFieldMeta entityField = entityFieldMap.get(fieldName);
                String cap = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                String entityVar = "__" + fieldName;

                String dtoQName = dtoField.getTypeQualifiedName();
                String entityQName = entityField.getTypeQualifiedName();

                // 集合类型：for-loop 元素转换
                if (isSameCollectionType(dtoQName, entityQName)) {
                    generateCollectionEntityConversion(builder, elements,
                            dtoQName, entityQName, "dto.get" + cap + "()", "entity", cap,
                            generatedFields);
                    continue;
                }

                // 普通对象：BeanUtils.copyProperties
                builder.beginControlFlow("if (dto.$L() != null)", "get" + cap);
                builder.addStatement("$T $L = new $T()",
                        ClassName.bestGuess(entityQName), entityVar,
                        ClassName.bestGuess(entityQName));
                builder.addStatement("$T.copyProperties(dto.$L(), $L)", BEAN_UTILS, "get" + cap, entityVar);

                // 递归处理更深层嵌套（集合 + 自引用）
                if (elements != null) {
                    generateBeanUtilsReverseRecursive(builder, elements,
                            dtoQName, entityQName,
                            "dto.get" + cap + "()", entityVar, generatedFields);
                }

                builder.addStatement("entity.$L($L)", "set" + cap, entityVar);
                builder.endControlFlow();
            }
        }

        builder.addStatement("repository.save(entity)")
                .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success(entity))",
                        SERVER_RESPONSE, MediaType.class, REST_RESPONSE)
                .addException(ServletException.class)
                .addException(IOException.class);
    }

    // ========================================================================
    //  嵌套 DTO 转换（同名字段不同类型自动映射，递归版本+集合支持+循环检测）
    // ========================================================================

    /** 标准库集合类 raw type */
    private static final Set<String> COLLECTION_RAW_TYPES = Set.of(
            "java.util.List", "java.util.Set", "java.util.Collection",
            "java.util.ArrayList", "java.util.HashSet", "java.util.LinkedHashSet");

    /**
     * 判断当前字段的两端 raw type 相同且均为集合。
     * 此时需要将具体类型参数（元素类型）提取出来单独转换。
     */
    private static boolean isSameCollectionType(String srcType, String tgtType) {
        String srcRaw = extractRawType(srcType);
        String tgtRaw = extractRawType(tgtType);
        return srcRaw.equals(tgtRaw) && COLLECTION_RAW_TYPES.contains(srcRaw);
    }

    /**
     * 为同名字段但类型不同的实体 → DTO 属性生成递归嵌套的 {@code BeanUtils.copyProperties} 调用。
     * <p>例：{@code User.address: Address → UserDto.address: AddressDto}，且
     * {@code Address.zip: ZipCode → AddressDto.zip: ZipCodeDto}：</p>
     * <pre>{@code
     * if (entity.getAddress() != null) {
     *     AddressDto __addressDto = new AddressDto();
     *     BeanUtils.copyProperties(entity.getAddress(), __addressDto);
     *     if (entity.getAddress().getZip() != null) {
     *         ZipCodeDto __zipDto = new ZipCodeDto();
     *         BeanUtils.copyProperties(entity.getAddress().getZip(), __zipDto);
     *         __addressDto.setZip(__zipDto);
     *     }
     *     dto.setAddress(__addressDto);
     * }
     * }</pre>
     *
     * @param elements 可为 null，此时降级为单层转换
     */
    private static void generateNestedDtoConversions(
            MethodSpec.Builder builder, RepositoryMeta meta, Elements elements,
            List<SchemaFieldMeta> entityFields,
            List<SchemaFieldMeta> dtoFields,
            String srcVar, String dstVar) {

        if (dtoFields == null || entityFields == null) return;

        // 构建实体字段名索引
        Map<String, SchemaFieldMeta> entityFieldMap = new HashMap<>();
        for (SchemaFieldMeta f : entityFields) {
            entityFieldMap.put(f.getFieldName(), f);
        }

        Set<String> generatedFields = new HashSet<>();

        for (SchemaFieldMeta dtoField : dtoFields) {
            String fieldName = dtoField.getFieldName();
            SchemaFieldMeta entityField = entityFieldMap.get(fieldName);
            if (entityField == null) continue;

            String entityQName = entityField.getTypeQualifiedName();
            String dtoQName = dtoField.getTypeQualifiedName();

            String dtoSimple = dtoField.getTypeSimpleName();
            String entitySimple = entityField.getTypeSimpleName();

            String cap = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String srcGetExpr = srcVar + ".get" + cap + "()";

            // 集合类型处理（如 Set<MenuResource> → Set<MenuResourceDetila>，simpleName 相同但类型参数不同）
            if (isSameCollectionType(entityQName, dtoQName)) {
                generateCollectionDtoConversion(builder, elements,
                        entityQName, dtoQName, srcGetExpr, dstVar, cap,
                        generatedFields);
                continue;
            }

            // 类型相同 → 无需转换
            if (entitySimple.equals(dtoSimple)) continue;
            // 目标端为标量 → 无法转换
            if (isScalarSimpleName(dtoSimple)) continue;

            // 普通对象类型：生成 if-block
            String nestedDstVar = "__" + fieldName + "Dto";
            builder.beginControlFlow("if ($L != null)", srcGetExpr);
            builder.addStatement("$T $L = new $T()",
                    ClassName.bestGuess(dtoQName), nestedDstVar,
                    ClassName.bestGuess(dtoQName));
            builder.addStatement("$T.copyProperties($L, $L)", BEAN_UTILS, srcGetExpr, nestedDstVar);

            if (elements != null) {
                generateBeanUtilsDtoRecursive(builder, elements,
                        entityQName, dtoQName,
                        srcGetExpr, nestedDstVar, generatedFields);
            }

            builder.addStatement("$L.$L($L)", dstVar, "set" + cap, nestedDstVar);
            builder.endControlFlow();
        }
    }

    /**
     * 递归生成实体 → DTO 深层的 nested BeanUtils.copyProperties 调用。
     * <p>不依赖 {@code visitedPairs} —— 仅通过 {@code generatedFields} 的
     * {@code pairKey#fieldName} 键去重，同一类型同级同名字段只处理一次，
     * 从而允许自引用类型（如 parent.parent）递归到更深层。</p>
     */
    private static void generateBeanUtilsDtoRecursive(
            MethodSpec.Builder builder, Elements elements,
            String sourceTypeQName, String targetTypeQName,
            String srcGetExpr, String dstVar,
            Set<String> generatedFields) {

        if (isNonIntrospectable(sourceTypeQName, elements)
                || isNonIntrospectable(targetTypeQName, elements)) {
            return;
        }

        String srcRaw = extractRawType(sourceTypeQName);
        String tgtRaw = extractRawType(targetTypeQName);

        // 集合类型：生成元素级 for-loop 转换
        if (isSameCollectionType(sourceTypeQName, targetTypeQName)) {
            List<String> srcParams = extractTypeParameters(sourceTypeQName);
            List<String> tgtParams = extractTypeParameters(targetTypeQName);
            if (srcParams.size() == 1 && tgtParams.size() == 1
                    && !srcParams.get(0).equals(tgtParams.get(0))
                    && !isNonIntrospectable(srcParams.get(0), elements)
                    && !isNonIntrospectable(tgtParams.get(0), elements)) {
                String loopVar = "__" + lowerFirst(simpleName(srcParams.get(0)));
                String childVar = "__" + lowerFirst(simpleName(tgtParams.get(0)));
                builder.beginControlFlow("if ($L != null)", srcGetExpr);
                builder.addStatement("$T $L = new $T<>()",
                        ParameterizedTypeName.get(collectionInterfaceClass(tgtRaw),
                                ClassName.bestGuess(tgtParams.get(0))),
                        dstVar, collectionImplClass(tgtRaw));
                builder.beginControlFlow("for ($T $L : $L)",
                        ClassName.bestGuess(srcParams.get(0)), loopVar, srcGetExpr);
                builder.addStatement("$T $L = new $T()",
                        ClassName.bestGuess(tgtParams.get(0)), childVar,
                        ClassName.bestGuess(tgtParams.get(0)));
                builder.addStatement("$T.copyProperties($L, $L)", BEAN_UTILS, loopVar, childVar);
                generateBeanUtilsDtoRecursive(builder, elements,
                        srcParams.get(0), tgtParams.get(0),
                        loopVar, childVar, generatedFields);
                builder.addStatement("$L.add($L)", dstVar, childVar);
                builder.endControlFlow();
                builder.endControlFlow();
            }
            return;
        }

        // 同 raw type 但不是集合（如 Optional）→ 跳过
        if (srcRaw.equals(tgtRaw)) return;

        TypeElement srcEl = elements.getTypeElement(srcRaw);
        TypeElement tgtEl = elements.getTypeElement(tgtRaw);
        if (srcEl == null || tgtEl == null) return;

        String pairKey = sourceTypeQName + "→" + targetTypeQName;
        Map<String, String> srcFields = collectFields(srcEl, elements);
        Map<String, String> tgtFields = collectFields(tgtEl, elements);

        for (Map.Entry<String, String> entry : srcFields.entrySet()) {
            String fieldName = entry.getKey();
            String fieldPairKey = pairKey + "#" + fieldName;
            if (!generatedFields.add(fieldPairKey)) continue;

            String srcFieldType = entry.getValue();
            String tgtFieldType = tgtFields.get(fieldName);
            if (tgtFieldType == null || srcFieldType.equals(tgtFieldType)) continue;

            String cap = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String nestedDstVar = "__" + fieldName + "Dto";
            String nestedSrcExpr = srcGetExpr + ".get" + cap + "()";

            // 集合子字段优先（simpleName 相同但类型参数不同）
            if (isSameCollectionType(srcFieldType, tgtFieldType)) {
                generateCollectionDtoConversion(builder, elements,
                        srcFieldType, tgtFieldType, nestedSrcExpr, dstVar, cap,
                        generatedFields);
                continue;
            }

            String srcSimple = simpleName(srcFieldType);
            String tgtSimple = simpleName(tgtFieldType);
            if (srcSimple.equals(tgtSimple) || isScalarSimpleName(tgtSimple)) continue;

            // 普通对象字段
            builder.beginControlFlow("if ($L != null)", nestedSrcExpr);
            builder.addStatement("$T $L = new $T()",
                    ClassName.bestGuess(tgtFieldType), nestedDstVar,
                    ClassName.bestGuess(tgtFieldType));
            builder.addStatement("$T.copyProperties($L, $L)", BEAN_UTILS, nestedSrcExpr, nestedDstVar);

            generateBeanUtilsDtoRecursive(builder, elements, srcFieldType, tgtFieldType,
                    nestedSrcExpr, nestedDstVar, generatedFields);

            builder.addStatement("$L.$L($L)", dstVar, "set" + cap, nestedDstVar);
            builder.endControlFlow();
        }
    }

    /**
     * 为同名字段但类型不同的 DTO → Entity 属性生成递归嵌套的 {@code BeanUtils.copyProperties} 调用。
     * <p>例：{@code CreateUserDto.address: AddressDto → User.address: Address}，
     * 且 {@code AddressDto.zip: ZipCodeDto → Address.zip: ZipCode}：</p>
     * <pre>{@code
     * if (dto.getAddress() != null) {
     *     Address __address = new Address();
     *     BeanUtils.copyProperties(dto.getAddress(), __address);
     *     if (dto.getAddress().getZip() != null) {
     *         ZipCode __zip = new ZipCode();
     *         BeanUtils.copyProperties(dto.getAddress().getZip(), __zip);
     *         __address.setZip(__zip);
     *     }
     *     entity.setAddress(__address);
     * }
     * }</pre>
     */
    private static void generateReverseNestedDtoConversions(
            MethodSpec.Builder builder, RepositoryMeta meta, Elements elements,
            List<SchemaFieldMeta> dtoFields,
            List<SchemaFieldMeta> entityFields,
            String srcVar, String dstVar) {

        if (dtoFields == null || entityFields == null) return;

        // 构建实体字段名 → SchemaFieldMeta 索引
        Map<String, SchemaFieldMeta> entityFieldMap = new HashMap<>();
        for (SchemaFieldMeta f : entityFields) {
            entityFieldMap.put(f.getFieldName(), f);
        }

        Set<String> generatedFields = new HashSet<>();

        for (SchemaFieldMeta dtoField : dtoFields) {
            String fieldName = dtoField.getFieldName();
            SchemaFieldMeta entityField = entityFieldMap.get(fieldName);
            if (entityField == null) continue;

            String dtoQName = dtoField.getTypeQualifiedName();
            String entityQName = entityField.getTypeQualifiedName();
            String dtoSimple = dtoField.getTypeSimpleName();
            String entitySimple = entityField.getTypeSimpleName();

            String cap = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String srcGetExpr = srcVar + ".get" + cap + "()";

            // 集合类型优先（simpleName 相同但类型参数不同）
            if (isSameCollectionType(dtoQName, entityQName)) {
                generateCollectionEntityConversion(builder, elements,
                        dtoQName, entityQName, srcGetExpr, dstVar, cap,
                        generatedFields);
                continue;
            }

            // 类型相同 → 已被 BeanUtils.copyProperties 处理，跳过
            if (dtoSimple.equals(entitySimple)) continue;
            // DTO 端为标量 → 无法转换
            if (isScalarSimpleName(dtoSimple)) continue;

            // 普通对象类型
            String entityVar = "__" + fieldName;
            builder.beginControlFlow("if ($L != null)", srcGetExpr);
            builder.addStatement("$T $L = new $T()",
                    ClassName.bestGuess(entityQName), entityVar,
                    ClassName.bestGuess(entityQName));
            builder.addStatement("$T.copyProperties($L, $L)", BEAN_UTILS, srcGetExpr, entityVar);

            if (elements != null) {
                generateBeanUtilsReverseRecursive(builder, elements,
                        dtoQName, entityQName,
                        srcGetExpr, entityVar, generatedFields);
            }

            builder.addStatement("$L.$L($L)", dstVar, "set" + cap, entityVar);
            builder.endControlFlow();
        }
    }

    /**
     * 递归生成 DTO → Entity 深层的 nested BeanUtils.copyProperties 调用。
     * <p>不依赖 {@code visitedPairs} —— 仅通过 {@code generatedFields} 的
     * {@code pairKey#fieldName} 键去重，自引用类型（如 parent.parent）可递归到更深层。</p>
     */
    private static void generateBeanUtilsReverseRecursive(
            MethodSpec.Builder builder, Elements elements,
            String dtoTypeQName, String entityTypeQName,
            String srcGetExpr, String dstVar,
            Set<String> generatedFields) {

        if (isNonIntrospectable(dtoTypeQName, elements)
                || isNonIntrospectable(entityTypeQName, elements)) {
            return;
        }

        String dtoRaw = extractRawType(dtoTypeQName);
        String entityRaw = extractRawType(entityTypeQName);

        // 集合类型：生成元素级 for-loop 转换
        if (isSameCollectionType(dtoTypeQName, entityTypeQName)) {
            List<String> dtoParams = extractTypeParameters(dtoTypeQName);
            List<String> entityParams = extractTypeParameters(entityTypeQName);
            if (dtoParams.size() == 1 && entityParams.size() == 1
                    && !dtoParams.get(0).equals(entityParams.get(0))
                    && !isNonIntrospectable(dtoParams.get(0), elements)
                    && !isNonIntrospectable(entityParams.get(0), elements)) {
                String loopVar = "__" + lowerFirst(simpleName(dtoParams.get(0)));
                String childVar = "__" + lowerFirst(simpleName(entityParams.get(0)));
                builder.beginControlFlow("if ($L != null)", srcGetExpr);
                builder.addStatement("$T $L = new $T<>()",
                        ParameterizedTypeName.get(collectionInterfaceClass(entityRaw),
                                ClassName.bestGuess(entityParams.get(0))),
                        dstVar, collectionImplClass(entityRaw));
                builder.beginControlFlow("for ($T $L : $L)",
                        ClassName.bestGuess(dtoParams.get(0)), loopVar, srcGetExpr);
                builder.addStatement("$T $L = new $T()",
                        ClassName.bestGuess(entityParams.get(0)), childVar,
                        ClassName.bestGuess(entityParams.get(0)));
                builder.addStatement("$T.copyProperties($L, $L)", BEAN_UTILS, loopVar, childVar);
                generateBeanUtilsReverseRecursive(builder, elements,
                        dtoParams.get(0), entityParams.get(0),
                        loopVar, childVar, generatedFields);
                builder.addStatement("$L.add($L)", dstVar, childVar);
                builder.endControlFlow();
                builder.endControlFlow();
            }
            return;
        }

        // 同 raw type 但不是集合（如 Optional）→ 跳过
        if (dtoRaw.equals(entityRaw)) return;

        TypeElement dtoEl = elements.getTypeElement(dtoRaw);
        TypeElement entityEl = elements.getTypeElement(entityRaw);
        if (dtoEl == null || entityEl == null) return;

        String pairKey = dtoTypeQName + "→" + entityTypeQName;
        Map<String, String> dtoFields = collectFields(dtoEl, elements);
        Map<String, String> entityFields = collectFields(entityEl, elements);

        for (Map.Entry<String, String> entry : dtoFields.entrySet()) {
            String fieldName = entry.getKey();
            String fieldPairKey = pairKey + "#" + fieldName;
            if (!generatedFields.add(fieldPairKey)) continue;

            String dtoFieldType = entry.getValue();
            String entityFieldType = entityFields.get(fieldName);
            if (entityFieldType == null || dtoFieldType.equals(entityFieldType)) continue;

            String cap = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String nestedDstVar = "__" + fieldName;
            String nestedSrcExpr = srcGetExpr + ".get" + cap + "()";

            // 集合子字段优先（simpleName 相同但类型参数不同）
            if (isSameCollectionType(dtoFieldType, entityFieldType)) {
                generateCollectionEntityConversion(builder, elements,
                        dtoFieldType, entityFieldType, nestedSrcExpr, dstVar, cap,
                        generatedFields);
                continue;
            }

            String dtoSimple = simpleName(dtoFieldType);
            String entitySimple = simpleName(entityFieldType);
            if (dtoSimple.equals(entitySimple) || isScalarSimpleName(dtoSimple)) continue;

            // 普通对象字段
            builder.beginControlFlow("if ($L != null)", nestedSrcExpr);
            builder.addStatement("$T $L = new $T()",
                    ClassName.bestGuess(entityFieldType), nestedDstVar,
                    ClassName.bestGuess(entityFieldType));
            builder.addStatement("$T.copyProperties($L, $L)", BEAN_UTILS, nestedSrcExpr, nestedDstVar);

            generateBeanUtilsReverseRecursive(builder, elements, dtoFieldType, entityFieldType,
                    nestedSrcExpr, nestedDstVar, generatedFields);

            builder.addStatement("$L.$L($L)", dstVar, "set" + cap, nestedDstVar);
            builder.endControlFlow();
        }
    }

    // ========================================================================
    //  集合类型转换辅助方法
    // ========================================================================

    /** 根据集合 raw type 选择实现类 */
    private static ClassName collectionImplClass(String rawType) {
        if (rawType.equals("java.util.Set")
                || rawType.equals("java.util.HashSet")
                || rawType.equals("java.util.LinkedHashSet")) {
            return ClassName.get(java.util.LinkedHashSet.class);
        }
        return ClassName.get(ArrayList.class);
    }

    /** 根据集合 raw type 选择接口类型 */
    private static ClassName collectionInterfaceClass(String rawType) {
        if (rawType.equals("java.util.Set")
                || rawType.equals("java.util.HashSet")
                || rawType.equals("java.util.LinkedHashSet")) {
            return ClassName.get(java.util.Set.class);
        }
        return ClassName.get(List.class);
    }

    /**
     * 生成 Entity 侧集合字段 → DTO 侧集合的 for-loop 元素转换代码。
     * <pre>{@code
     * if (entity.getChildren() != null) {
     *     List<MenuResourceDetila> __children = new ArrayList<>(entity.getChildren().size());
     *     for (MenuResource __child : entity.getChildren()) {
     *         MenuResourceDetila __childDto = new MenuResourceDetila();
     *         BeanUtils.copyProperties(__child, __childDto);
     *         __children.add(__childDto);
     *     }
     *     dto.setChildren(__children);
     * }
     * }</pre>
     */
    private static void generateCollectionDtoConversion(
            MethodSpec.Builder builder, Elements elements,
            String entityTypeQName, String dtoTypeQName,
            String srcGetExpr, String dstVar, String cap,
            Set<String> generatedFields) {

        List<String> entityParams = extractTypeParameters(entityTypeQName);
        List<String> dtoParams = extractTypeParameters(dtoTypeQName);
        if (entityParams.size() != 1 || dtoParams.size() != 1) return;

        String elementEntity = entityParams.get(0);
        String elementDto = dtoParams.get(0);
        if (elementEntity.equals(elementDto)) return;
        if (isScalarSimpleName(simpleName(elementEntity))
                || isScalarSimpleName(simpleName(elementDto))) return;

        String targetRaw = extractRawType(dtoTypeQName);
        String listVar = "__" + cap.substring(0, 1).toLowerCase() + cap.substring(1) + "List";
        String loopVar = "__" + lowerFirst(simpleName(elementEntity));
        String childVar = "__" + lowerFirst(simpleName(elementDto));

        builder.beginControlFlow("if ($L != null)", srcGetExpr);
        builder.addStatement("$T $L = new $T<>()",
                ParameterizedTypeName.get(collectionInterfaceClass(targetRaw),
                        ClassName.bestGuess(elementDto)),
                listVar, collectionImplClass(targetRaw));
        builder.beginControlFlow("for ($T $L : $L)",
                ClassName.bestGuess(elementEntity), loopVar, srcGetExpr);
        builder.addStatement("$T $L = new $T()",
                ClassName.bestGuess(elementDto), childVar, ClassName.bestGuess(elementDto));
        builder.addStatement("$T.copyProperties($L, $L)", BEAN_UTILS, loopVar, childVar);

        if (elements != null) {
            generateBeanUtilsDtoRecursive(builder, elements,
                    elementEntity, elementDto,
                    loopVar, childVar, generatedFields);
        }

        builder.addStatement("$L.add($L)", listVar, childVar);
        builder.endControlFlow();
        builder.addStatement("$L.$L($L)", dstVar, "set" + cap, listVar);
        builder.endControlFlow();
    }

    /**
     * 生成 DTO 侧集合字段 → Entity 侧集合的 for-loop 元素转换代码。
     * <pre>{@code
     * if (dto.getChildren() != null) {
     *     List<MenuResource> __childrenList = new ArrayList<>(dto.getChildren().size());
     *     for (MenuResourceDetila __childDto : dto.getChildren()) {
     *         MenuResource __child = new MenuResource();
     *         BeanUtils.copyProperties(__childDto, __child);
     *         __childrenList.add(__child);
     *     }
     *     entity.setChildren(__childrenList);
     * }
     * }</pre>
     */
    private static void generateCollectionEntityConversion(
            MethodSpec.Builder builder, Elements elements,
            String dtoTypeQName, String entityTypeQName,
            String srcGetExpr, String dstVar, String cap,
            Set<String> generatedFields) {

        List<String> dtoParams = extractTypeParameters(dtoTypeQName);
        List<String> entityParams = extractTypeParameters(entityTypeQName);
        if (dtoParams.size() != 1 || entityParams.size() != 1) return;

        String elementDto = dtoParams.get(0);
        String elementEntity = entityParams.get(0);
        if (elementDto.equals(elementEntity)) return;
        if (isScalarSimpleName(simpleName(elementDto))
                || isScalarSimpleName(simpleName(elementEntity))) return;

        String targetRaw = extractRawType(entityTypeQName);
        String listVar = "__" + cap.substring(0, 1).toLowerCase() + cap.substring(1) + "List";
        String loopVar = "__" + lowerFirst(simpleName(elementDto));
        String childVar = "__" + lowerFirst(simpleName(elementEntity));

        builder.beginControlFlow("if ($L != null)", srcGetExpr);
        builder.addStatement("$T $L = new $T<>()",
                ParameterizedTypeName.get(collectionInterfaceClass(targetRaw),
                        ClassName.bestGuess(elementEntity)),
                listVar, collectionImplClass(targetRaw));
        builder.beginControlFlow("for ($T $L : $L)",
                ClassName.bestGuess(elementDto), loopVar, srcGetExpr);
        builder.addStatement("$T $L = new $T()",
                ClassName.bestGuess(elementEntity), childVar, ClassName.bestGuess(elementEntity));
        builder.addStatement("$T.copyProperties($L, $L)", BEAN_UTILS, loopVar, childVar);

        if (elements != null) {
            generateBeanUtilsReverseRecursive(builder, elements,
                    elementDto, elementEntity,
                    loopVar, childVar, generatedFields);
        }

        builder.addStatement("$L.add($L)", listVar, childVar);
        builder.endControlFlow();
        builder.addStatement("$L.$L($L)", dstVar, "set" + cap, listVar);
        builder.endControlFlow();
    }

    // ========================================================================
    //  DELETE
    // ========================================================================

    private static void buildDeleteMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName idType = meta.getIdType();

        builder.addAnnotation(
                AnnotationSpec.builder(TRANSACTIONAL)
                        .addMember(
                                "rollbackFor",
                                "$T.class",
                                Exception.class)
                        .build())
                .addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idType, ClassName.get(Long.class), "id")
                .addStatement("repository.deleteById(id)")
                .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success())",
                        SERVER_RESPONSE, MediaType.class, REST_RESPONSE);
    }

    // ========================================================================
    //  SORT（批量重排序）
    // ========================================================================

    /**
     * 生成批量重排序 Handler 方法。
     * <p>接受 {@code BaseSortReq[]} 请求体，逐条更新实体的排序字段。
     * 排序字段名由 {@code @QueryOrder(autoSort = true)} 标注的字段决定。</p>
     */
    private static void buildSortBatchMethod(
            RepositoryMeta meta,
            MethodSpec.Builder builder,
            boolean useMapStruct) {

        QueryFieldMeta sortField = meta.getQueryFields().stream()
                .filter(QueryFieldMeta::isAutoSort)
                .findFirst()
                .orElse(null);

        if (sortField == null) {
            return;
        }

        builder.addAnnotation(
                AnnotationSpec.builder(TRANSACTIONAL)
                        .addMember(
                                "rollbackFor",
                                "$T.class",
                                Exception.class)
                        .build());

        TypeName entityType = meta.getEntityType();

        String fieldName = sortField.getFieldName();

        String cap =
                Character.toUpperCase(fieldName.charAt(0))
                        + fieldName.substring(1);

        String typeQName = sortField.getTypeQualifiedName();

        ClassName baseSortReq =
                ClassName.get(
                        "com.github.zeng.alt.api.base",
                        "BaseSortReq");

        TypeName baseSortReqArray =
                ArrayTypeName.of(baseSortReq);

        TypeName idList =
                ParameterizedTypeName.get(
                        ClassName.get(List.class),
                        ClassName.get(Long.class));

        TypeName entityList =
                ParameterizedTypeName.get(
                        ClassName.get(List.class),
                        entityType);

        TypeName entityMap =
                ParameterizedTypeName.get(
                        ClassName.get(Map.class),
                        ClassName.get(Long.class),
                        entityType);

        // BaseSortReq[] reqArray = request.body(BaseSortReq[].class);
        builder.addStatement(
                "$T reqArray = request.body($T.class)",
                baseSortReqArray,
                baseSortReqArray);

        // List<Long> ids = Arrays.stream(reqArray).map(BaseSortReq::getId).toList();
        builder.addStatement(
                "$T ids = $T.stream(reqArray).map($T::getId).toList()",
                idList,
                Arrays.class,
                baseSortReq);

        // List<Entity> entities = repository.findByIdIn(ids);
        builder.addStatement(
                "$T entities = repository.findByIdIn(ids)",
                entityList);

        // Map<Long, Entity> entityMap = entities.stream().collect(Collectors.toMap(Entity::getId, Function.identity()));
        builder.addStatement(
                "$T entityMap = entities.stream().collect($T.toMap($T::getId, $T.identity()))",
                entityMap,
                Collectors.class,
                entityType,
                java.util.function.Function.class);

        // for (...)
        builder.beginControlFlow(
                "for ($T req : reqArray)",
                baseSortReq);

        builder.addStatement(
                "$T entity = entityMap.get(req.getId())",
                entityType);

        builder.beginControlFlow(
                "if (entity == null)");

        builder.addStatement("continue");

        builder.endControlFlow();

        String setterExpr = switch (typeQName) {
            case "java.lang.Long", "long" ->
                    "req.getSort().longValue()";
            case "java.lang.Double", "double" ->
                    "req.getSort().doubleValue()";
            case "java.math.BigDecimal" ->
                    "new java.math.BigDecimal(req.getSort())";
            default ->
                    "req.getSort()";
        };

        builder.addStatement(
                "entity.set$L($L)",
                cap,
                setterExpr);

        builder.endControlFlow();

        // repository.saveAll(entities);
        builder.addStatement(
                "repository.saveAll(entities)");

        // return ...
        builder.addStatement(
                "return $T.ok().contentType($T.APPLICATION_JSON).body($T.success())",
                SERVER_RESPONSE,
                MediaType.class,
                REST_RESPONSE);

        builder.addException(ServletException.class)
                .addException(IOException.class);
    }

    // ========================================================================
    //  QueryDSL Predicate 构建
    // ========================================================================

    private static MethodSpec buildPredicateMethod(RepositoryMeta meta) {
        ClassName qClassName = getQClassName(meta);
        String instanceName = getEntityInstanceName(meta);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("buildPredicate")
                .addModifiers(Modifier.PRIVATE)
                .returns(PREDICATE)
                .addParameter(ParameterSpec.builder(SERVER_REQUEST, "request").build())
                .addStatement("$T builder = new $T()", BOOLEAN_BUILDER, BOOLEAN_BUILDER)
                .addStatement("$T $L = $T.$L", qClassName, instanceName, qClassName, instanceName);

        for (QueryFieldMeta field : meta.getQueryFields()) {
            if (field.isRange()) {
                generateRangeCondition(methodBuilder, field, instanceName);
            } else if (field.isMulti()) {
                generateMultiCondition(methodBuilder, field, instanceName);
            } else {
                generateSimpleCondition(methodBuilder, field, instanceName);
            }
        }

        methodBuilder.addStatement("return builder");
        return methodBuilder.build();
    }

    private static void generateSimpleCondition(MethodSpec.Builder builder, QueryFieldMeta field, String instanceName) {
        String valueExpr = field.getLikeWrappedExpr("__v");
        String finalExpr = field.getConversionExpr(valueExpr);

        builder.addStatement("request.param($S).ifPresent(__v -> builder.and($L.$L.$L($L)))",
                field.getColumn(), instanceName, field.getFieldName(),
                field.getQueryMethod(), finalExpr);
    }

    private static void generateMultiCondition(MethodSpec.Builder builder, QueryFieldMeta field, String instanceName) {
        builder.addStatement("request.param($S).ifPresent(__v -> builder.and($L.$L.in((Object[]) __v.split($S))))",
                field.getColumn(), instanceName, field.getFieldName(), ",");
    }

    private static void generateRangeCondition(MethodSpec.Builder builder, QueryFieldMeta field, String instanceName) {
        String startParam = field.getRangeStart().isEmpty() ? field.getFieldName() + "Start" : field.getRangeStart();
        String endParam = field.getRangeEnd().isEmpty() ? field.getFieldName() + "End" : field.getRangeEnd();

        ParameterizedTypeName optionalStringType =
                ParameterizedTypeName.get(ClassName.get(Optional.class), ClassName.get(String.class));

        builder.addStatement("$T __rangeStart = request.param($S)", optionalStringType, startParam)
                .addStatement("$T __rangeEnd = request.param($S)", optionalStringType, endParam)
                .beginControlFlow("if (__rangeStart.isPresent() && __rangeEnd.isPresent())")
                .addStatement("builder.and($L.$L.between($L, $L))",
                        instanceName, field.getFieldName(),
                        field.getConversionExpr("__rangeStart.get()"),
                        field.getConversionExpr("__rangeEnd.get()"))
                .endControlFlow();
    }

    // ========================================================================
    //  Sort 构建
    // ========================================================================

    private static MethodSpec buildSortMethod(RepositoryMeta meta) {
        List<QueryFieldMeta> orderFields = meta.getQueryFields().stream()
                .filter(QueryFieldMeta::isHasOrder)
                .sorted(Comparator.comparingInt(QueryFieldMeta::getOrderPriority))
                .toList();

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("buildSort")
                .addModifiers(Modifier.PRIVATE)
                .returns(SORT);

        if (orderFields.size() == 1) {
            QueryFieldMeta field = orderFields.get(0);
            methodBuilder.addStatement("return $T.by($T.$L, $S)",
                    SORT, SORT_DIRECTION,
                    field.isOrderAsc() ? "ASC" : "DESC",
                    field.getFieldName());
        } else {
            for (int i = 0; i < orderFields.size(); i++) {
                QueryFieldMeta field = orderFields.get(i);
                String direction = field.isOrderAsc() ? "ASC" : "DESC";
                if (i == 0) {
                    methodBuilder.addStatement("$T sort = $T.by($T.$L, $S)",
                            SORT, SORT, SORT_DIRECTION, direction, field.getFieldName());
                } else {
                    methodBuilder.addStatement("sort = sort.and($T.by($T.$L, $S))",
                            SORT, SORT_DIRECTION, direction, field.getFieldName());
                }
            }
            methodBuilder.addStatement("return sort");
        }

        return methodBuilder.build();
    }

    // ========================================================================
    //  工具方法
    // ========================================================================

    /** 将首字母转为小写，用于变量命名 */
    private static String lowerFirst(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static ClassName getQClassName(RepositoryMeta meta) {
        ClassName entityType = meta.getEntityType();
        return ClassName.get(entityType.packageName(), "Q" + entityType.simpleName());
    }

    private static String getEntityInstanceName(RepositoryMeta meta) {
        String name = meta.getEntitySimpleName();
        if (name.isEmpty()) {
            return "entity";
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
