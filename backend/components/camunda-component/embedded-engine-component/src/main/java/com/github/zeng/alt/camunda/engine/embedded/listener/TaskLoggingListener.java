package com.github.zeng.alt.camunda.engine.embedded.listener;

import lombok.extern.slf4j.Slf4j;
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;


/**
 * 通用任务监听器骨架
 * <p>
 * 监听用户任务的 create / assignment / complete / delete 事件，默认打印日志，
 * 子类可按需重写对应钩子方法实现业务逻辑。
 * <p>
 * 在 BPMN 中通过 Spring Bean 表达式引用：
 * {@code <camunda:taskListener event="create" delegateExpression="${taskLoggingListener}"/>}
 *
 * @author zengAlt
 */
@Component("taskLoggingListener")
@Slf4j
public class TaskLoggingListener implements TaskListener {

    public static final String BUSINESS_KEY = "businessKey";

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        log.info("任务事件 [{}] taskId={}, taskDefinitionKey={}, taskName={}, assignee={}, processInstanceId={}, processDefinitionId={}, executionId={}", eventName, delegateTask.getId(), delegateTask.getTaskDefinitionKey(), delegateTask.getName(), delegateTask.getAssignee(), delegateTask.getProcessInstanceId(), delegateTask.getProcessDefinitionId(), delegateTask.getExecutionId());
    }

    /**
     * 任务创建时触发，可在此实现自动分配办理人、通知等逻辑
     */
    protected void onCreate(DelegateTask delegateTask) {
    }

    /**
     * 办理人发生变更时触发（签收、转办、改派等）
     */
    protected void onAssignment(DelegateTask delegateTask) {
    }

    /**
     * 任务完成前触发，可在此校验表单完整性或记录审批意见
     */
    protected void onComplete(DelegateTask delegateTask) {
    }

    /**
     * 任务被删除时触发（流程终止、跳过等）
     */
    protected void onDelete(DelegateTask delegateTask) {
    }
}
