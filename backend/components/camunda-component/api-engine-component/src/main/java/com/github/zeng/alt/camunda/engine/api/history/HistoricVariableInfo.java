package com.github.zeng.alt.camunda.engine.api.history;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 历史变量信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class HistoricVariableInfo implements Serializable {

    private String id;
    private String name;
    private Object value;
    private String type;
    private String processInstanceId;
    private String executionId;
    private String activityInstanceId;
    private Integer revision;
}
