package com.github.zeng.alt.rest.apt.meta;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository 元模型 — 存储扫描和校验后的信息
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 1.0
 */
public class RepositoryMeta {

    private final String repositorySimpleName;
    private final String repositoryPackageName;
    private final String generatedPackageName;
    private final ClassName entityType;
    private final ClassName idType;
    private final String path;
    private final boolean pageable;
    private final List<MethodMeta> enabledMethods;
    private final TypeElement repositoryElement;

    private RepositoryMeta(Builder builder) {
        this.repositorySimpleName = builder.repositorySimpleName;
        this.repositoryPackageName = builder.repositoryPackageName;
        this.generatedPackageName = builder.generatedPackageName;
        this.entityType = builder.entityType;
        this.idType = builder.idType;
        this.path = builder.path;
        this.pageable = builder.pageable;
        this.enabledMethods = Collections.unmodifiableList(new ArrayList<>(builder.enabledMethods));
        this.repositoryElement = builder.repositoryElement;
    }

    public String getRepositorySimpleName() {
        return repositorySimpleName;
    }

    public String getRepositoryPackageName() {
        return repositoryPackageName;
    }

    public String getGeneratedPackageName() {
        return generatedPackageName;
    }

    public ClassName getEntityType() {
        return entityType;
    }

    public ClassName getIdType() {
        return idType;
    }

    public String getPath() {
        return path;
    }

    public boolean isPageable() {
        return pageable;
    }

    public List<MethodMeta> getEnabledMethods() {
        return enabledMethods;
    }

    public TypeElement getRepositoryElement() {
        return repositoryElement;
    }

    public String getEntitySimpleName() {
        return entityType.simpleName();
    }

    public String getHandlerSimpleName() {
        return getEntitySimpleName() + "Handler";
    }

    public String getRouterSimpleName() {
        return getEntitySimpleName() + "Router";
    }

    public String getRepositorySimpleNameUncapitalized() {
        String name = repositorySimpleName;
        if (name.endsWith("Repository")) {
            name = name.substring(0, name.length() - "Repository".length());
        }
        if (name.isEmpty()) {
            name = repositorySimpleName;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String repositorySimpleName;
        private String repositoryPackageName;
        private String generatedPackageName;
        private ClassName entityType;
        private ClassName idType;
        private String path;
        private boolean pageable = true;
        private final List<MethodMeta> enabledMethods = new ArrayList<>();
        private TypeElement repositoryElement;

        private Builder() {
        }

        public Builder repositorySimpleName(String repositorySimpleName) {
            this.repositorySimpleName = repositorySimpleName;
            return this;
        }

        public Builder repositoryPackageName(String repositoryPackageName) {
            this.repositoryPackageName = repositoryPackageName;
            return this;
        }

        public Builder generatedPackageName(String generatedPackageName) {
            this.generatedPackageName = generatedPackageName;
            return this;
        }

        public Builder entityType(ClassName entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder idType(ClassName idType) {
            this.idType = idType;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder pageable(boolean pageable) {
            this.pageable = pageable;
            return this;
        }

        public Builder addEnabledMethod(MethodMeta method) {
            this.enabledMethods.add(method);
            return this;
        }

        public Builder repositoryElement(TypeElement repositoryElement) {
            this.repositoryElement = repositoryElement;
            return this;
        }

        public RepositoryMeta build() {
            return new RepositoryMeta(this);
        }
    }
}
