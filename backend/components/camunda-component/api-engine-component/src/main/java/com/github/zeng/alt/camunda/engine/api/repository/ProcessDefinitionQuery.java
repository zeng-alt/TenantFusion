package com.github.zeng.alt.camunda.engine.api.repository;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 流程定义查询
 *
 * @author zengAlt
 */
@Data
@Builder
public class ProcessDefinitionQuery implements Serializable {

    private String key;
    private String name;
    private Boolean suspended;
    private Boolean latestVersion;
    private String tenantId;
    @Builder.Default
    private Integer pageNo = 1;
    @Builder.Default
    private Integer pageSize = 10;
}
