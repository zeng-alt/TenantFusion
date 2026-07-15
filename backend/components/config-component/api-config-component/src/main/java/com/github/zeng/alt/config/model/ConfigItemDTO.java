package com.github.zeng.alt.config.model;

import java.io.Serializable;

public class ConfigItemDTO implements Serializable {

    private Long configId;
    private String dataId;
    private String group;
    private String content;
    private String format;
    private Integer version;
    private String appCode;

    public ConfigItemDTO() {
    }

    public ConfigItemDTO(String dataId, String group, String content, String format) {
        this.dataId = dataId;
        this.group = group;
        this.content = content;
        this.format = format;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }
}
