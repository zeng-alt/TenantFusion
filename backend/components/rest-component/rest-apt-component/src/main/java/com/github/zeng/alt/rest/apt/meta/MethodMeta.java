package com.github.zeng.alt.rest.apt.meta;

/**
 * 方法元模型 — 描述单个 CRUD 方法的信息
 *
 * @author zengJiaJun
 * @since 2026年05月28日
 * @version 1.0
 */
public enum MethodMeta {

    SEARCH("search", "GET", "/search", false),
    SEARCH_BODY("search", "POST", "/search", true),
    LIST("list", "GET", "", false),
    LIST_ALL("listAll", "POST", "/all", false),
    DETAIL("detail", "GET", "/{id:\\d+}", false),
    CREATE("create", "POST", "", true),
    UPDATE("update", "PUT", "/{id:\\d+}", true),
    PATCH("patch", "PATCH", "/{id:\\d+}", true),
    DELETE("delete", "DELETE", "/{ids}", false),
    SORT("sort", "PUT", "/sort", true),
    ;


    private final String methodName;
    private final String httpMethod;
    private final String routeSuffix;
    private final boolean hasRequestBody;

    MethodMeta(String methodName, String httpMethod, String routeSuffix, boolean hasRequestBody) {
        this.methodName = methodName;
        this.httpMethod = httpMethod;
        this.routeSuffix = routeSuffix;
        this.hasRequestBody = hasRequestBody;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRouteSuffix() {
        return routeSuffix;
    }

    public boolean isHasRequestBody() {
        return hasRequestBody;
    }
}
