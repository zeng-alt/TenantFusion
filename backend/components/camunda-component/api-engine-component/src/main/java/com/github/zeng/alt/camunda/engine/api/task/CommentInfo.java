package com.github.zeng.alt.camunda.engine.api.task;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务批注信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class CommentInfo implements Serializable {

    private String id;
    private String taskId;
    private String processInstanceId;
    private String userId;
    private String message;
    private LocalDateTime time;
}
