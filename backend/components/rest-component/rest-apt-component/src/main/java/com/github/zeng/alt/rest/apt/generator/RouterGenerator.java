package com.github.zeng.alt.rest.apt.generator;

import com.github.zeng.alt.rest.apt.meta.MethodMeta;
import com.github.zeng.alt.rest.apt.meta.QueryFieldMeta;
import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.github.zeng.alt.rest.apt.meta.SchemaFieldMeta;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Router 生成器 — 使用 JavaPoet 生成 Router Configuration 类。
 * <p>职责：生成 {@link RouterFunction} Bean 和 SpringDoc 的 {@code OpenApiCustomizer}。</p>
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 2.0
 */
public final class RouterGenerator {

    private static final ClassName SERVER_RESPONSE = ClassName.get(ServerResponse.class);
    private static final ClassName ROUTER_FUNCTIONS = ClassName.get(RouterFunctions.class);
    private static final ParameterizedTypeName ROUTER_FUNCTION_TYPE =
            ParameterizedTypeName.get(ClassName.get(RouterFunction.class), SERVER_RESPONSE);
    private static final ClassName OPEN_API_CUSTOMIZER =
            ClassName.get("org.springdoc.core.customizers", "OpenApiCustomizer");
    private static final ClassName OPEN_API = ClassName.get("io.swagger.v3.oas.models", "OpenAPI");
    private static final ClassName PATH_ITEM = ClassName.get("io.swagger.v3.oas.models", "PathItem");
    private static final ClassName API_RESPONSES =
            ClassName.get("io.swagger.v3.oas.models.responses", "ApiResponses");
    private static final ClassName API_RESPONSE =
            ClassName.get("io.swagger.v3.oas.models.responses", "ApiResponse");
    private static final ClassName CONTENT =
            ClassName.get("io.swagger.v3.oas.models.media", "Content");
    private static final ClassName MEDIA_TYPE =
            ClassName.get("io.swagger.v3.oas.models.media", "MediaType");
    private static final ClassName SCHEMA =
            ClassName.get("io.swagger.v3.oas.models.media", "Schema");
    private static final ClassName REQUEST_BODY =
            ClassName.get("io.swagger.v3.oas.models.parameters", "RequestBody");
    private static final ClassName OPERATION =
            ClassName.get("io.swagger.v3.oas.models", "Operation");
    private static final ClassName LIST = ClassName.get("java.util", "List");
    private static final ClassName TAG = ClassName.get("io.swagger.v3.oas.models.tags", "Tag");
    private static final ClassName ARRAY_SCHEMA =
            ClassName.get("io.swagger.v3.oas.models.media", "ArraySchema");
    private static final ClassName INTEGER_SCHEMA =
            ClassName.get("io.swagger.v3.oas.models.media", "IntegerSchema");
    private static final ClassName PARAMETER =
            ClassName.get("io.swagger.v3.oas.models.parameters", "Parameter");
    private static final ClassName COMPONENTS =
            ClassName.get("io.swagger.v3.oas.models", "Components");

    private RouterGenerator() {}

    public static JavaFile generate(RepositoryMeta meta) {
        String beanName = meta.getRepositorySimpleNameUncapitalized() + "Route";
        String handlerParamName = meta.getRepositorySimpleNameUncapitalized() + "Handler";

        // Build formatted code string: return RouterFunctions.route().GET(...).POST(...).build()
        StringBuilder codeFormat = new StringBuilder("return $T.route()");
        for (MethodMeta ignored : meta.getEnabledMethods()) {
            codeFormat.append("\n    .$L($S, $L)");
        }
        codeFormat.append("\n    .build()");

        // Build all arguments
        Object[] args = new Object[meta.getEnabledMethods().size() * 3 + 1];
        args[0] = ROUTER_FUNCTIONS;
        int argIndex = 1;
        for (MethodMeta method : meta.getEnabledMethods()) {
            args[argIndex++] = method.getHttpMethod();
            args[argIndex++] = meta.getPath() + method.getRouteSuffix();
            args[argIndex++] = handlerParamName + "::" + resolveHandlerMethodName(method);
        }

        MethodSpec routerMethod = MethodSpec.methodBuilder(beanName)
                .addAnnotation(Bean.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ROUTER_FUNCTION_TYPE)
                .addParameter(ClassName.bestGuess(meta.getHandlerSimpleName()), handlerParamName)
                .addStatement(codeFormat.toString(), args)
                .build();

        TypeSpec.Builder routerBuilder = TypeSpec.classBuilder(meta.getRouterSimpleName())
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", "rest-apt-component")
                        .build())
                .addAnnotation(Configuration.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(routerMethod);

        if (meta.isHasSpringDoc()) {
            routerBuilder.addType(buildSpringDocConfig(meta));
        }

        return JavaFile.builder(meta.getGeneratedPackageName(), routerBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    // ========================================================================
    //  OpenApiCustomizer — 嵌套 @Configuration
    // ========================================================================

    private static TypeSpec buildSpringDocConfig(RepositoryMeta meta) {
        String configName = meta.getRepositorySimpleName() + "SpringDocConfiguration";
        String customizerName = meta.getRepositorySimpleNameUncapitalized() + "ApiCustomizer";
        String customizeMethodName = "customize" + meta.getEntitySimpleName() + "Api";

        ClassName conditionalOnClass = ClassName.get(
                "org.springframework.boot.autoconfigure.condition", "ConditionalOnClass");

        TypeSpec.Builder configBuilder = TypeSpec.classBuilder(configName)
                .addModifiers(Modifier.STATIC)
                .addAnnotation(AnnotationSpec.builder(Configuration.class)
                        .addMember("proxyBeanMethods", "$L", false)
                        .build())
                .addAnnotation(AnnotationSpec.builder(conditionalOnClass)
                        .addMember("name", "$S",
                                "org.springdoc.core.customizers.OpenApiCustomizer")
                        .build())
                .addMethod(MethodSpec.methodBuilder(customizerName)
                        .addAnnotation(Bean.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(OPEN_API_CUSTOMIZER)
                        .addStatement("return this::$L", customizeMethodName)
                        .build());

        configBuilder.addMethod(buildCustomizerMethod(meta));
        return configBuilder.build();
    }

    // ========================================================================
    //  OpenApiCustomizer — 定制方法生成
    // ========================================================================

    private static MethodSpec buildCustomizerMethod(RepositoryMeta meta) {
        String methodName = "customize" + meta.getEntitySimpleName() + "Api";
        String entityName = meta.getEntitySimpleName();
        String tagName = entityName + " Controller";

        CodeBlock.Builder body = CodeBlock.builder();

        // =============================
        // 1. 注册 Schema 组件
        // =============================
        body.beginControlFlow("if (openApi.getComponents() == null)");
        body.addStatement("openApi.setComponents(new $T())", COMPONENTS);
        body.endControlFlow();

        String entityRef = "#/components/schemas/" + entityName;
        String pageResponseName = entityName + "PageResponse";
        String responseName = entityName + "Response";

        // {Entity}PageResponse
        {
            String dataVar = "__" + entityName + "PageData";
            body.addStatement("$T $L = new $T().items(new $T<>().$$ref($S))",
                    ARRAY_SCHEMA, dataVar, ARRAY_SCHEMA, SCHEMA, entityRef);
            body.addStatement("openApi.getComponents().addSchemas($S, new $T<>().type($S)" +
                            ".addProperty($S, new $T().description($S))" +
                            ".addProperty($S, $L)" +
                            ".addProperty($S, new $T().description($S))" +
                            ".addProperty($S, new $T().description($S))" +
                            ".addProperty($S, new $T().description($S)))",
                    pageResponseName,
                    SCHEMA, "object",
                    "status", INTEGER_SCHEMA, "状态码",
                    "data", dataVar,
                    "pageNum", INTEGER_SCHEMA, "页码",
                    "pageSize", INTEGER_SCHEMA, "每页条数",
                    "total", INTEGER_SCHEMA, "总记录数");
        }

        // {Entity}Response
        body.addStatement("openApi.getComponents().addSchemas($S, new $T<>().type($S)" +
                        ".addProperty($S, new $T().description($S))" +
                        ".addProperty($S, new $T<>().$$ref($S)))",
                responseName,
                SCHEMA, "object",
                "status", INTEGER_SCHEMA, "状态码",
                "data", SCHEMA, entityRef);

        // 实体 Schema（含字段属性）
        registerSchemaIfAbsent(body, entityName,
                OpenApiSchemaGenerator.buildObjectSchemaWithFields(entityName, meta.getEntityFields()));

        // Create/Update 专用 Schema
        if (meta.getCreateType() != null && !meta.getCreateType().equals(meta.getEntityType())) {
            String name = meta.getCreateType().simpleName();
            CodeBlock schema = meta.getCreateTypeFields().isEmpty()
                    ? OpenApiSchemaGenerator.emptyObjectSchema(entityName + " 创建参数")
                    : OpenApiSchemaGenerator.buildObjectSchemaWithFields(name, meta.getCreateTypeFields());
            registerSchemaIfAbsent(body, name, schema);
        }
        if (meta.getUpdateType() != null && !meta.getUpdateType().equals(meta.getEntityType())) {
            String name = meta.getUpdateType().simpleName();
            CodeBlock schema = meta.getUpdateTypeFields().isEmpty()
                    ? OpenApiSchemaGenerator.emptyObjectSchema(entityName + " 更新参数")
                    : OpenApiSchemaGenerator.buildObjectSchemaWithFields(name, meta.getUpdateTypeFields());
            registerSchemaIfAbsent(body, name, schema);
        }
        // 注册 Patch 专用 Schema（优先使用 patchType，若不填则退化为 updateType 或 entity）
        {
            ClassName patchSchemaType = meta.getPatchType() != null ? meta.getPatchType()
                    : (meta.getUpdateType() != null ? meta.getUpdateType() : null);
            if (patchSchemaType != null && !patchSchemaType.equals(meta.getEntityType())) {
                String name = patchSchemaType.simpleName();
                registerSchemaIfAbsent(body, name,
                        OpenApiSchemaGenerator.emptyObjectSchema(entityName + " 部分更新参数"));
            }
        }

        // swapSort 开启时注册 BaseSortReq Schema
        if (meta.isSort() && hasAutoSort(meta)) {
            registerSchemaIfAbsent(body, "BaseSortReq",
                    CodeBlock.builder()
                            .add("new $T<>().type($S).description($S)" +
                                            ".addProperty($S, new $T<>().type($S).format($S).description($S))" +
                                            ".addProperty($S, new $T<>().type($S).format($S).description($S))",
                                    SCHEMA, "object", "排序请求体",
                                    "id", SCHEMA, "integer", "int64", "主键",
                                    "sort", SCHEMA, "integer", "int32", "排序值")
                            .build());
        }

        // search 开启时注册 JPASearchInput Schema
        if (meta.getEnabledMethods().contains(MethodMeta.SEARCH_BODY)) {
            // 注册内部 Schema
            registerSchemaIfAbsent(body, "JPASearchOptions",
                    CodeBlock.builder()
                            .add("new $T<>()\n", SCHEMA)
                            .add(".type($S)\n", "object")
                            .add(".addProperty($S, new $T<>().type($S))\n", "pageSize", SCHEMA, "integer")
                            .add(".addProperty($S, new $T<>().type($S))\n", "pageOffset", SCHEMA, "integer")
                            .add(".addProperty($S, new $T<>().type($S).items(new $T<>().$$ref($S)))\n",
                                    "sortOptions", SCHEMA, "array", SCHEMA, "#/components/schemas/JPASortOptions")
                            .add(".addProperty($S, new $T<>().type($S).items(new $T<>().type($S)))\n",
                                    "selections", SCHEMA, "array", SCHEMA, "string")
                            .build());

            registerSchemaIfAbsent(body, "JPASortOptions",
                    CodeBlock.builder()
                            .add("new $T<>()\n", SCHEMA)
                            .add(".type($S)\n", "object")
                            .add(".addProperty($S, new $T<>().type($S))\n", "key", SCHEMA, "string")
                            .add(".addProperty($S, new $T<>().type($S)._default(false))\n", "desc", SCHEMA, "boolean")
                            .build());

            registerSchemaIfAbsent(body, "FilterSingleValue",
                    CodeBlock.builder()
                            .add("new $T<>()\n", SCHEMA)
                            .add(".type($S)\n", "object")
                            .add(".addProperty($S, new $T<>().type($S))\n", "operator", SCHEMA, "string")
                            .add(".addProperty($S, new $T<>().type($S))\n", "key", SCHEMA, "string")
                            .add(".addProperty($S, new $T<>().$$ref($S))\n", "options", SCHEMA, "#/components/schemas/JPASearchFilterOptions")
                            .add(".addProperty($S, new $T<>())\n", "value", SCHEMA) // object 类型，允许任意值
                            .build());

            registerSchemaIfAbsent(body, "FilterMultipleValues",
                    CodeBlock.builder()
                            .add("new $T<>()\n", SCHEMA)
                            .add(".type($S)\n", "object")
                            .add(".addProperty($S, new $T<>().type($S))\n", "operator", SCHEMA, "string")
                            .add(".addProperty($S, new $T<>().type($S))\n", "key", SCHEMA, "string")
                            .add(".addProperty($S, new $T<>().$$ref($S))\n", "options", SCHEMA, "#/components/schemas/JPASearchFilterOptions")
                            .add(".addProperty($S, new $T<>().type($S).items(new $T<>()))\n", "values", SCHEMA, "array", SCHEMA)
                            .build());

            registerSchemaIfAbsent(body, "RootFilter",
                    CodeBlock.builder()
                            .add("new $T<>()\n", SCHEMA)
                            .add(".type($S)\n", "object")
                            .add(".addProperty($S, new $T<>().type($S))\n", "operator", SCHEMA, "string")
                            .add(".addProperty($S, new $T<>().type($S).items(\n", "filters", SCHEMA, "array")
                            .add("    new $T<>().oneOf(java.util.Arrays.asList(\n", SCHEMA)
                            .add("        new $T<>().$$ref($S),\n", SCHEMA, "#/components/schemas/FilterSingleValue")
                            .add("        new $T<>().$$ref($S),\n", SCHEMA, "#/components/schemas/FilterMultipleValues")
                            .add("        new $T<>().$$ref($S)\n", SCHEMA, "#/components/schemas/RootFilter")
                            .add("    ))\n")
                            .add("))\n")
                            .build());

            registerSchemaIfAbsent(body, "JPASearchFilterOptions",
                    CodeBlock.builder()
                            .add("new $T<>()\n", SCHEMA)
                            .add(".type($S)\n", "object")
                            .add(".addProperty($S, new $T<>().type($S))\n", "ignoreCase", SCHEMA, "boolean")
                            .add(".addProperty($S, new $T<>().type($S))\n", "trim", SCHEMA, "boolean")
                            .add(".addProperty($S, new $T<>().type($S))\n", "negate", SCHEMA, "boolean")
                            .build());

            // 注册主 Schema，引用已注册的子类型
            registerSchemaIfAbsent(body, "JPASearchInput",
                    CodeBlock.builder()
                            .add("new $T<>()\n", SCHEMA)
                            .add(".type($S)\n", "object")
                            .add(".description($S)\n", "jpa-search-helper 搜索输入")
                            .add(".addProperty($S, new $T<>().$$ref($S))\n", "filter", SCHEMA, "#/components/schemas/RootFilter")
                            .add(".addProperty($S, new $T<>().$$ref($S))\n", "options", SCHEMA, "#/components/schemas/JPASearchOptions")
                            .build());
        }

        // =============================
        // 2. 注册 Tag
        // =============================
        body.addStatement("openApi.addTagsItem(new $T().name($S).description($S))",
                TAG, tagName, entityName + " CRUD 接口");

        // =============================
        // 3. 按路径分组生成 PathItem
        // =============================
        Map<String, List<MethodMeta>> methodsByPath = new LinkedHashMap<>();
        for (MethodMeta method : meta.getEnabledMethods()) {
            methodsByPath.computeIfAbsent(
                    meta.getPath() + method.getRouteSuffix(),
                    k -> new java.util.ArrayList<>()
            ).add(method);
        }

        int pathIndex = 0;
        for (Map.Entry<String, List<MethodMeta>> entry : methodsByPath.entrySet()) {
            String path = entry.getKey();
            String pathItemVar = "__pi" + pathIndex++;

            body.addStatement("$T $L = new $T()", PATH_ITEM, pathItemVar, PATH_ITEM);

            for (MethodMeta method : entry.getValue()) {
                String httpMethod = method.getHttpMethod();
                String setter = "set" + httpMethod.charAt(0) + httpMethod.substring(1).toLowerCase();
                String opVar = "__" + resolveHandlerMethodName(method);
                String summary = chineseSummary(entityName, method);
                String description = chineseDescription(entityName, method);
                String responseRef = "#/components/schemas/"
                        + (method == MethodMeta.LIST || method == MethodMeta.SEARCH || method == MethodMeta.SEARCH_BODY
                            ? pageResponseName : responseName);
                String requestBodyRef = null;
                if (method == MethodMeta.CREATE || method == MethodMeta.UPDATE || method == MethodMeta.PATCH) {
                    ClassName type;
                    if (method == MethodMeta.CREATE) {
                        type = meta.getCreateType() != null ? meta.getCreateType() : meta.getEntityType();
                    } else if (method == MethodMeta.UPDATE) {
                        type = meta.getUpdateType() != null ? meta.getUpdateType() : meta.getEntityType();
                    } else { // PATCH
                        type = meta.getPatchType() != null ? meta.getPatchType()
                                : (meta.getUpdateType() != null ? meta.getUpdateType() : meta.getEntityType());
                    }
                    requestBodyRef = "#/components/schemas/" + type.simpleName();
                } else if (method == MethodMeta.SORT) {
                    requestBodyRef = "#/components/schemas/BaseSortReq";
                } else if (method == MethodMeta.SEARCH_BODY) {
                    requestBodyRef = "#/components/schemas/JPASearchInput";
                }

                String respVar = opVar + "Resp";
                body.addStatement("$T $L = new $T().addApiResponse($S, new $T()" +
                                ".description($S)" +
                                ".content(new $T().addMediaType($S, new $T().schema(new $T().$$ref($S)))))",
                        API_RESPONSES, respVar, API_RESPONSES, "200",
                        API_RESPONSE, "成功",
                        CONTENT, "application/json",
                        MEDIA_TYPE, SCHEMA, responseRef);

                body.addStatement("$T $L = new $T()" +
                                ".summary($S)" +
                                ".description($S)" +
                                ".tags($T.of($S))" +
                                ".responses($L)",
                        OPERATION, opVar, OPERATION,
                        summary, description,
                        LIST, tagName,
                        respVar);

                if (method == MethodMeta.SEARCH) {
                    // 构建 Operation 时，不调用 addParametersItem
                    body.addStatement("$L.description($S)", opVar,
                            "使用 jpa-search-helper 搜索 Person，支持的所有查询参数请参考 JPASearchInput 扁平化规则。"
                                    + "示例: firstName=Biagio&lastName_startsWith=Toz&birthDate_gte=19910101&country_in=IT,FR,DE");
                }

                // 查询参数（仅 LIST 和 LIST_ALL）
                if (method == MethodMeta.LIST) {
                    body.addStatement("$L.addParametersItem(new $T().name($S).in($S).description($S).schema(new $T().$L(1).minimum(new java.math.BigDecimal(1))))",
                            opVar, PARAMETER, "pageNo", "query", "页码，从1开始", INTEGER_SCHEMA, "_default");
                    body.addStatement("$L.addParametersItem(new $T().name($S).in($S).description($S).schema(new $T().$L(10).minimum(new java.math.BigDecimal(1))))",
                            opVar, PARAMETER, "pageSize", "query", "每页条数", INTEGER_SCHEMA, "_default");
                }

                if (method == MethodMeta.LIST || method == MethodMeta.LIST_ALL) {
                    for (QueryFieldMeta field : meta.getQueryFields()) {
                        String col = field.getColumn();
                        CodeBlock fieldSchema = OpenApiSchemaGenerator.buildQueryFieldSchema(field);
                        String baseDesc = field.getDescription();
                        if (baseDesc.isEmpty()) {
                            baseDesc = field.getFieldName();
                        }
                        if (field.isRange()) {
                            String startParam = field.getRangeStart().isEmpty() ? col : field.getRangeStart();
                            String endParam = field.getRangeEnd().isEmpty() ? col : field.getRangeEnd();
                            body.addStatement("$L.addParametersItem(new $T().name($S).in($S).description($S).schema($L))",
                                    opVar, PARAMETER, startParam, "query", baseDesc + "起始", fieldSchema);
                            body.addStatement("$L.addParametersItem(new $T().name($S).in($S).description($S).schema($L))",
                                    opVar, PARAMETER, endParam, "query", baseDesc + "结束", fieldSchema);
                        } else if (field.isMulti()) {
                            body.addStatement("$L.addParametersItem(new $T().name($S).in($S).description($S).schema($L))",
                                    opVar, PARAMETER, col, "query", baseDesc + "(多值逗号分隔)", fieldSchema);
                        } else {
                            body.addStatement("$L.addParametersItem(new $T().name($S).in($S).description($S).schema($L))",
                                    opVar, PARAMETER, col, "query", baseDesc, fieldSchema);
                        }
                    }
                }

                // RequestBody
                if (requestBodyRef != null) {
                    String reqVar = opVar + "Req";
                    body.addStatement("$T $L = new $T()" +
                                    ".content(new $T().addMediaType($S, new $T().schema(new $T().$$ref($S))))" +
                                    ".required(true)",
                            REQUEST_BODY, reqVar, REQUEST_BODY,
                            CONTENT, "application/json",
                            MEDIA_TYPE, SCHEMA, requestBodyRef);
                    body.addStatement("$L.setRequestBody($L)", opVar, reqVar);
                }

                // Path 参数 {id}
                if (method.getRouteSuffix().contains("{id}")) {
                    body.addStatement("$L.addParametersItem(new $T().name($S).in($S).description($S).schema(new $T<>()))",
                            opVar, PARAMETER, "id", "path", "主键 ID", SCHEMA);
                }

                // 批量删除 Path 参数
                if (method == MethodMeta.DELETE) {
                    body.addStatement("$L.addParametersItem(new $T().name($S).in($S).description($S).required(true).schema(new $T<>()))",
                            opVar, PARAMETER, "ids", "path", "要删除的 ID 列表，逗号分隔，最多 100 个", SCHEMA);
                }

                body.addStatement("$L.$L($L)", pathItemVar, setter, opVar);
            }

            body.addStatement("openApi.path($S, $L)", path, pathItemVar);
        }

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE)
                .returns(TypeName.VOID)
                .addParameter(OPEN_API, "openApi")
                .addCode(body.build())
                .build();
    }

    // ========================================================================
    //  辅助方法
    // ========================================================================

    /**
     * 生成 if-not-contains-key 包裹的 addSchemas 调用，消除重复的 Schema 注册模板代码。
     */
    private static void registerSchemaIfAbsent(CodeBlock.Builder body, String schemaName, CodeBlock schemaExpr) {
        body.beginControlFlow("if (openApi.getComponents().getSchemas() == null || !openApi.getComponents().getSchemas().containsKey($S))",
                schemaName);
        body.addStatement("openApi.getComponents().addSchemas($S, $L)", schemaName, schemaExpr);
        body.endControlFlow();
    }

    /**
     * 检查实体是否有 {@code @QueryOrder(autoSort = true)} 字段。
     */
    private static boolean hasAutoSort(RepositoryMeta meta) {
        return meta.getQueryFields().stream().anyMatch(QueryFieldMeta::isAutoSort);
    }

    /**
     * 解析 Handler 方法名，{@link MethodMeta#SEARCH_BODY} 需要特殊处理避免与 {@link MethodMeta#SEARCH} 冲突。
     */
    private static String resolveHandlerMethodName(MethodMeta method) {
        if (method == MethodMeta.SEARCH_BODY) {
            return "searchBody";
        }
        return method.getMethodName();
    }

    private static String chineseSummary(String entityName, MethodMeta method) {
        return switch (method) {
            case LIST -> "分页查询" + entityName + "列表";
            case LIST_ALL -> "条件查询所有" + entityName + "（不分页）";
            case DETAIL -> "获取" + entityName + "详情";
            case CREATE -> "新增" + entityName;
            case UPDATE -> "全量更新" + entityName;
            case PATCH -> "部分更新" + entityName;
            case DELETE -> "批量删除" + entityName;
            case SORT -> "批量重排序" + entityName;
            case SEARCH -> "搜索" + entityName + "（GET）";
            case SEARCH_BODY -> "搜索" + entityName + "（POST）";
        };
    }

    private static String chineseDescription(String entityName, MethodMeta method) {
        return switch (method) {
            case LIST -> "分页条件查询" + entityName + "，支持多字段过滤和排序";
            case LIST_ALL -> "条件查询所有" + entityName + "，支持多字段过滤和排序，不分页";
            case DETAIL -> "根据 ID 获取" + entityName + "的详细信息";
            case CREATE -> "创建新的" + entityName + "记录";
            case UPDATE -> "全量更新已有" + entityName + "，未传字段会被置为 null";
            case PATCH -> "部分更新已有" + entityName + "，仅更新非 null 字段";
            case DELETE -> "批量删除" + entityName + "，逗号分隔 ID，最多 100 个";
            case SORT -> "批量更新" + entityName + "的排序值，请求体为 BaseSortReq 数组";
            case SEARCH -> "使用 jpa-search-helper 搜索" + entityName + "，查询参数通过 URL query params 传递";
            case SEARCH_BODY -> "使用 jpa-search-helper 搜索" + entityName + "，查询条件通过请求体传递";
        };
    }
}
