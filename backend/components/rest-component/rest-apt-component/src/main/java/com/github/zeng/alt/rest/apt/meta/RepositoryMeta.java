package com.github.zeng.alt.rest.apt.meta;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    private final boolean sort;
    private final List<MethodMeta> enabledMethods;
    private final TypeElement repositoryElement;
    private final ClassName queryType;
    private final ClassName createType;
    private final ClassName updateType;
    private final ClassName patchType;
    private final ClassName detailType;
    private final ClassName listType;
    private final List<QueryFieldMeta> queryFields;
    private final boolean hasQueryFields;
    private final boolean hasSpringDoc;
    private final List<SchemaFieldMeta> entityFields;
    private final List<SchemaFieldMeta> entityAllFields;   // 含 @JsonIgnore，用于 DTO 转换
    private final List<SchemaFieldMeta> createTypeFields;
    private final List<SchemaFieldMeta> updateTypeFields;
    private final List<SchemaFieldMeta> patchTypeFields;
    private final List<SchemaFieldMeta> listTypeFields;
    private final List<SchemaFieldMeta> detailTypeFields;

    private RepositoryMeta(Builder builder) {
        this.repositorySimpleName = builder.repositorySimpleName;
        this.repositoryPackageName = builder.repositoryPackageName;
        this.generatedPackageName = builder.generatedPackageName;
        this.entityType = builder.entityType;
        this.idType = builder.idType;
        this.path = builder.path;
        this.pageable = builder.pageable;
        this.sort = builder.sort;
        this.enabledMethods = Collections.unmodifiableList(new ArrayList<>(builder.enabledMethods));
        this.repositoryElement = builder.repositoryElement;
        this.queryType = builder.queryType;
        this.createType = builder.createType;
        this.updateType = builder.updateType;
        this.patchType = builder.patchType;
        this.detailType = builder.detailType;
        this.listType = builder.listType;
        this.queryFields = Collections.unmodifiableList(new ArrayList<>(builder.queryFields));
        this.hasQueryFields = !builder.queryFields.isEmpty();
        this.hasSpringDoc = builder.hasSpringDoc;
        this.entityFields = Collections.unmodifiableList(new ArrayList<>(builder.entityFields));
        this.entityAllFields = Collections.unmodifiableList(new ArrayList<>(builder.entityAllFields));
        this.createTypeFields = Collections.unmodifiableList(new ArrayList<>(builder.createTypeFields));
        this.updateTypeFields = Collections.unmodifiableList(new ArrayList<>(builder.updateTypeFields));
        this.patchTypeFields = Collections.unmodifiableList(new ArrayList<>(builder.patchTypeFields));
        this.listTypeFields = Collections.unmodifiableList(new ArrayList<>(builder.listTypeFields));
        this.detailTypeFields = Collections.unmodifiableList(new ArrayList<>(builder.detailTypeFields));
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

    public boolean isSort() {
        return sort;
    }

    public List<MethodMeta> getEnabledMethods() {
        return enabledMethods;
    }

    public TypeElement getRepositoryElement() {
        return repositoryElement;
    }

    public ClassName getQueryType() {
        return queryType;
    }

    public List<QueryFieldMeta> getQueryFields() {
        return queryFields;
    }

    public boolean isHasQueryFields() {
        return hasQueryFields;
    }

    public ClassName getCreateType() {
        return createType;
    }

    public ClassName getUpdateType() {
        return updateType;
    }

    public ClassName getPatchType() {
        return patchType;
    }

    public ClassName getDetailType() {
        return detailType;
    }

    public ClassName getListType() {
        return listType;
    }

    public boolean isHasSpringDoc() {
        return hasSpringDoc;
    }

    public List<SchemaFieldMeta> getEntityFields() {
        return entityFields;
    }

    public List<SchemaFieldMeta> getEntityAllFields() {
        return entityAllFields;
    }

    public List<SchemaFieldMeta> getCreateTypeFields() {
        return createTypeFields;
    }

    public List<SchemaFieldMeta> getUpdateTypeFields() {
        return updateTypeFields;
    }

    public List<SchemaFieldMeta> getPatchTypeFields() {
        return patchTypeFields;
    }

    public List<SchemaFieldMeta> getListTypeFields() {
        return listTypeFields;
    }

    public List<SchemaFieldMeta> getDetailTypeFields() {
        return detailTypeFields;
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
        private boolean sort = false;
        private final List<MethodMeta> enabledMethods = new ArrayList<>();
        private TypeElement repositoryElement;
        private ClassName queryType;
        private ClassName createType;
        private ClassName updateType;
        private ClassName patchType;
        private ClassName detailType;
        private ClassName listType;
        private boolean hasSpringDoc;
        private final List<QueryFieldMeta> queryFields = new ArrayList<>();
        private final List<SchemaFieldMeta> entityFields = new ArrayList<>();
        private final List<SchemaFieldMeta> entityAllFields = new ArrayList<>();
        private final List<SchemaFieldMeta> createTypeFields = new ArrayList<>();
        private final List<SchemaFieldMeta> updateTypeFields = new ArrayList<>();
        private final List<SchemaFieldMeta> patchTypeFields = new ArrayList<>();
        private final List<SchemaFieldMeta> listTypeFields = new ArrayList<>();
        private final List<SchemaFieldMeta> detailTypeFields = new ArrayList<>();

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

        public Builder sort(boolean sort) {
            this.sort = sort;
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

        public Builder queryType(ClassName queryType) {
            this.queryType = queryType;
            return this;
        }

        public Builder createType(ClassName createType) {
            this.createType = createType;
            return this;
        }

        public Builder updateType(ClassName updateType) {
            this.updateType = updateType;
            return this;
        }

        public Builder patchType(ClassName patchType) {
            this.patchType = patchType;
            return this;
        }

        public Builder detailType(ClassName detailType) {
            this.detailType = detailType;
            return this;
        }

        public Builder listType(ClassName listType) {
            this.listType = listType;
            return this;
        }

        public Builder hasSpringDoc(boolean hasSpringDoc) {
            this.hasSpringDoc = hasSpringDoc;
            return this;
        }

        public Builder addQueryField(QueryFieldMeta queryField) {
            this.queryFields.add(queryField);
            return this;
        }

        public Builder queryFields(List<QueryFieldMeta> queryFields) {
            this.queryFields.clear();
            this.queryFields.addAll(queryFields);
            return this;
        }

        public Builder addEntityField(SchemaFieldMeta field) {
            this.entityFields.add(field);
            return this;
        }

        public Builder entityFields(List<SchemaFieldMeta> entityFields) {
            this.entityFields.clear();
            this.entityFields.addAll(entityFields);
            return this;
        }

        public Builder addEntityAllField(SchemaFieldMeta field) {
            this.entityAllFields.add(field);
            return this;
        }

        public Builder entityAllFields(List<SchemaFieldMeta> entityAllFields) {
            this.entityAllFields.clear();
            this.entityAllFields.addAll(entityAllFields);
            return this;
        }

        public Builder addCreateTypeField(SchemaFieldMeta field) {
            this.createTypeFields.add(field);
            return this;
        }

        public Builder createTypeFields(List<SchemaFieldMeta> createTypeFields) {
            this.createTypeFields.clear();
            this.createTypeFields.addAll(createTypeFields);
            return this;
        }

        public Builder addUpdateTypeField(SchemaFieldMeta field) {
            this.updateTypeFields.add(field);
            return this;
        }

        public Builder updateTypeFields(List<SchemaFieldMeta> updateTypeFields) {
            this.updateTypeFields.clear();
            this.updateTypeFields.addAll(updateTypeFields);
            return this;
        }

        public Builder addPatchTypeField(SchemaFieldMeta field) {
            this.patchTypeFields.add(field);
            return this;
        }

        public Builder patchTypeFields(List<SchemaFieldMeta> patchTypeFields) {
            this.patchTypeFields.clear();
            this.patchTypeFields.addAll(patchTypeFields);
            return this;
        }

        public Builder addListTypeField(SchemaFieldMeta field) {
            this.listTypeFields.add(field);
            return this;
        }

        public Builder listTypeFields(List<SchemaFieldMeta> listTypeFields) {
            this.listTypeFields.clear();
            this.listTypeFields.addAll(listTypeFields);
            return this;
        }

        public Builder addDetailTypeField(SchemaFieldMeta field) {
            this.detailTypeFields.add(field);
            return this;
        }

        public Builder detailTypeFields(List<SchemaFieldMeta> detailTypeFields) {
            this.detailTypeFields.clear();
            this.detailTypeFields.addAll(detailTypeFields);
            return this;
        }

        public RepositoryMeta build() {
            return new RepositoryMeta(this);
        }
    }
}
