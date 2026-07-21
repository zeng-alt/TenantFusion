package com.github.zeng.alt.security.rbac.serve.router;

import java.util.List;

public class RouteTemplateEvent {

    private String contextPath;
    private List<String> templates;

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public List<String> getTemplates() {
        return templates;
    }

    public void setTemplates(List<String> templates) {
        this.templates = templates;
    }
}
