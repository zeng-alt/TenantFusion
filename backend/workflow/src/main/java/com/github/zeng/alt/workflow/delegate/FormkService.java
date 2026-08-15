package com.github.zeng.alt.workflow.delegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.zeng.alt.json.JacksonHelper;
import com.github.zeng.alt.workflow.model.GlobalFormDataSubmitCmd;
import com.github.zeng.alt.workflow.model.GlobalFormDataVO;
import com.github.zeng.alt.workflow.service.GlobalFormDataService;
import io.holunda.camunda.bpm.data.CamundaBpmData;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 流程全局表单数据操作服务（表单任务）。
 * <p>
 * 用于 BPMN 服务任务，通过 {@code camunda:delegateExpression="${formkService}"} 引用，
 * 配合设计器模板 {@code camunda7-ui:form-task} 使用。在服务任务的
 * {@code camunda:inputOutput} 中声明操作指令，按前缀区分操作类型：
 * <ul>
 *     <li>{@code formk:add:<字段名>}：新增/覆盖全局表单数据中的字段。
 *         取值支持字面量（{@code <camunda:inputParameter name="formk:add:test">test</...>}）、
 *         表达式引用（{@code ${someVar}} 或 {@code #{someVar}}）以及
 *         {@code camunda:source="someVar"}，均由引擎解析为实际值。</li>
 *     <li>{@code formk:delete:<字段名>}：删除全局表单数据中的字段
 *         （{@code <camunda:inputParameter name="formk:delete:test" />}）。</li>
 * </ul>
 * 引擎的 IO 映射会将上述输入参数解析为当前执行上的局部流程变量，本服务通过
 * camunda-bpm-data（{@link CamundaBpmData}）读取这些变量，再对当前流程实例已初始化的
 * 全局表单数据（{@link GlobalFormDataService}）原地增删后提交；未配置任何操作指令时仅记录日志，
 * 不影响流程执行。
 *
 * @author zengAlt
 */
@Component("formkService")
@RequiredArgsConstructor
@CommonsLog
public class FormkService implements JavaDelegate {

    private static final String ADD_PREFIX = "formk:add:";
    private static final String DELETE_PREFIX = "formk:delete:";

    private final GlobalFormDataService globalFormDataService;
    private final JacksonHelper jacksonHelper;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        GlobalFormDataVO existing = globalFormDataService.getByProcessInstanceId(processInstanceId);
        ObjectNode data = toObjectNode(existing);

        boolean changed = applyOperations(execution, data);
        if (!changed) {
            log.info("全局表单数据操作: processInstanceId=" + processInstanceId + ", 无有效操作指令，跳过提交");
            return;
        }

        GlobalFormDataSubmitCmd cmd = new GlobalFormDataSubmitCmd();
        cmd.setProcessInstanceId(processInstanceId);
        cmd.setWorkflowCode(resolveWorkflowCode(execution, existing));
        cmd.setData(jacksonHelper.toJsonString(data));
        globalFormDataService.submit(cmd);
        log.info("全局表单数据操作完成: processInstanceId=" + processInstanceId + ", data=" + cmd.getData());
    }

    /**
     * 扫描局部流程变量并执行 add/delete 操作
     *
     * @param execution 流程执行上下文
     * @param data      全局表单数据（原地修改）
     * @return 是否存在有效变更
     */
    private boolean applyOperations(DelegateExecution execution, ObjectNode data) {
        boolean changed = false;
        for (String name : execution.getVariableNamesLocal()) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (name.startsWith(ADD_PREFIX)) {
                data.set(name.substring(ADD_PREFIX.length()), toJsonNode(readInput(execution, name)));
                changed = true;
            } else if (name.startsWith(DELETE_PREFIX)) {
                if (data.remove(name.substring(DELETE_PREFIX.length())) != null) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * 读取输入参数对应的局部流程变量值
     */
    private Object readInput(DelegateExecution execution, String name) {
        return CamundaBpmData.customVariable(name, Object.class).from(execution).getLocalOrNull();
    }

    /**
     * 将任意对象转换为 JSON 节点；null 转为 NullNode
     */
    private JsonNode toJsonNode(Object value) {
        return jacksonHelper.getObjectMapper().valueToTree(value);
    }

    /**
     * 解析流程模板编码：优先取已存在的全局表单数据，否则从流程定义ID（key:version:id）解析
     */
    private String resolveWorkflowCode(DelegateExecution execution, GlobalFormDataVO existing) {
        if (existing != null && StringUtils.hasText(existing.getWorkflowCode())) {
            return existing.getWorkflowCode();
        }
        String processDefinitionId = execution.getProcessDefinitionId();
        if (StringUtils.hasText(processDefinitionId)) {
            return processDefinitionId.split(":")[0];
        }
        return null;
    }

    /**
     * 将已有全局表单数据解析为可变对象节点；不存在或非对象时返回空对象
     */
    private ObjectNode toObjectNode(GlobalFormDataVO existing) {
        if (existing != null && existing.getData() instanceof ObjectNode objectNode) {
            return objectNode.deepCopy();
        }
        return jacksonHelper.getObjectMapper().createObjectNode();
    }
}
