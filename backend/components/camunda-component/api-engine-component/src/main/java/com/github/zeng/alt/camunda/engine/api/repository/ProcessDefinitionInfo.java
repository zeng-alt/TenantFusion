package com.github.zeng.alt.camunda.engine.api.repository;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程定义信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class ProcessDefinitionInfo implements Serializable {

    private String id;
    private String key;
    private String name;
    private Integer version;
    private String description;
    private String category;
    private Boolean suspended;
    private String deploymentId;
    private String resourceName;
    private String diagramResourceName;
    private String tenantId;
}
