package com.github.zeng.alt.config.model;

import java.io.Serializable;
import java.util.Objects;

public class ConfigChangedKey implements Serializable {

    private String dataId;
    private String group;

    public ConfigChangedKey() {
    }

    public ConfigChangedKey(String dataId, String group) {
        this.dataId = dataId;
        this.group = group;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfigChangedKey that)) return false;
        return Objects.equals(dataId, that.dataId) && Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataId, group);
    }
}
