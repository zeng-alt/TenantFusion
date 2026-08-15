package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import com.github.zeng.alt.workflow.entity.WorkflowEntity;
import com.github.zeng.alt.workflow.entity.WorkflowVersionEntity;
import com.github.zeng.alt.workflow.model.GlobalFormDefinitionVO;
import com.github.zeng.alt.workflow.repository.WorkflowRepository;
import com.github.zeng.alt.workflow.repository.WorkflowVersionRepository;
import com.github.zeng.alt.workflow.service.GlobalFormDefinitionService;
import com.github.zeng.alt.workflow.service.GlobalFormResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 流程全局表单定义服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class GlobalFormDefinitionServiceImpl implements GlobalFormDefinitionService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final ProcessDefinitionApi processDefinitionApi;
    private final GlobalFormResolver globalFormResolver;

    @Override
    public GlobalFormDefinitionVO resolveByWorkflowCode(String workflowCode) {
        if (!StringUtils.hasText(workflowCode)) {
            return null;
        }
        WorkflowEntity workflow = workflowRepository.findByWorkflowKey(workflowCode).orElse(null);
        if (workflow == null) {
            return null;
        }
        WorkflowVersionEntity version = workflowVersionRepository
                .findFirstByWorkflowIdAndCurrentTrue(workflow.getWorkflowId())
                .orElse(null);
        return globalFormResolver.resolve(loadBpmnXml(version));
    }

    /**
     * 加载流程版本的 BPMN XML：已发布版本本地不存 XML，从 Camunda 部署模型读取
     */
    private String loadBpmnXml(WorkflowVersionEntity version) {
        if (version == null) {
            return null;
        }
        if (StringUtils.hasText(version.getBpmnXml())) {
            return version.getBpmnXml();
        }
        if (StringUtils.hasText(version.getProcessDefinitionId())) {
            try {
                byte[] xml = processDefinitionApi.getBpmnXml(version.getProcessDefinitionId());
                return xml == null ? null : new String(xml, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("加载流程定义BPMN失败: " + version.getProcessDefinitionId(), e);
                return null;
            }
        }
        return null;
    }
}
