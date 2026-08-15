package com.github.zeng.alt.camunda.engine.api.task;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务查询
 *
 * @author zengAlt
 */
@Data
@Builder
public class TaskQuery implements Serializable {

    private String name;
    private String processDefinitionName;
    /**
     * 待办用户ID（办理人或候选人，与 assignee/candidateUser 取并集）
     */
    private String userId;
    /**
     * 发起人用户ID
     */
    private String initiator;
    private String taskDefinitionKey;
    private String assignee;
    private String candidateUser;
    private String candidateGroup;
    private String processInstanceId;
    private String processDefinitionKey;
    private String businessKey;
    private Boolean suspended;
    private Boolean unassigned;
    private String tenantId;
    @Builder.Default
    private Integer pageNo = 1;
    @Builder.Default
    private Integer pageSize = 10;
}
