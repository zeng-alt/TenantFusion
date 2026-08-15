package com.github.zeng.alt.camunda.engine.api.process;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 流程启动结果信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class ProcessInformation implements Serializable {

    public static final String META_PROCESS_DEFINITION_KEY = "processDefinitionKey";
    public static final String META_BUSINESS_KEY = "businessKey";
    public static final String META_TENANT_ID = "tenantId";
    public static final String META_ROOT_PROCESS_INSTANCE_ID = "rootProcessInstanceId";
    public static final String META_PROCESS_DEFINITION_ID = "processDefinitionId";

    /**
     * 流程实例ID
     */
    private String instanceId;

    /**
     * 元数据（processDefinitionKey/businessKey/tenantId 等）
     */
    private Map<String, String> meta;
}
