package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.camunda.engine.api.deploy.DeployBundleCommand;
import com.github.zeng.alt.camunda.engine.api.deploy.DeploymentApi;
import com.github.zeng.alt.camunda.engine.api.deploy.DeploymentInformation;
import com.github.zeng.alt.camunda.engine.api.deploy.NamedResource;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionApi;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionInfo;
import com.github.zeng.alt.camunda.engine.api.repository.ProcessDefinitionQuery;
import com.github.zeng.alt.workflow.model.ProcessDefinitionVO;
import com.github.zeng.alt.workflow.service.ProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * 流程定义服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private final ProcessDefinitionApi processDefinitionApi;
    private final DeploymentApi deploymentApi;

    @Override
    public PageRestResponse<ProcessDefinitionVO> queryDefinitions(String key, String name, Boolean suspended,
                                                                  Boolean latestVersion, int pageNum, int pageSize) {
        PageRestResponse<ProcessDefinitionInfo> page = processDefinitionApi.query(ProcessDefinitionQuery.builder()
                .key(key)
                .name(name)
                .suspended(suspended)
                .latestVersion(latestVersion)
                .pageNo(pageNum)
                .pageSize(pageSize)
                .build());
        List<ProcessDefinitionVO> vos = page.getData().getPageData().stream().map(this::toVO).toList();
        return PageRestResponse.of(vos, page.getData().getTotal(), pageSize, pageNum);
    }

    @Override
    public ProcessDefinitionVO getDefinition(String id) {
        return toVO(processDefinitionApi.get(id));
    }

    @Override
    public DeploymentInformation deploy(MultipartFile bpmnXml) throws IOException {
        NamedResource resource = new NamedResource(
                Objects.requireNonNull(bpmnXml.getOriginalFilename()),
                bpmnXml.getBytes()
        );
        return deploymentApi.deploy(DeployBundleCommand.builder()
                .resources(List.of(resource))
                .build());
    }

    @Override
    @Deprecated
    public ProcessDefinitionVO deploy(String name, String bpmnXml, String tenantId) {
        DeploymentInformation deployment = deploymentApi.deploy(DeployBundleCommand.builder()
                .resources(List.of(new NamedResource("process.bpmn", bpmnXml.getBytes(StandardCharsets.UTF_8))))
                .tenantId(tenantId)
                .build());
        ProcessDefinitionInfo pd = processDefinitionApi.getByDeploymentId(deployment.getDeploymentId());
        if (pd == null) {
            throw new IllegalStateException("部署成功但未找到流程定义");
        }
        log.info("部署流程定义成功: " + pd.getKey() + " v" + pd.getVersion());
        return toVO(pd);
    }

    @Override
    public void deleteDefinition(String id, boolean cascade) {
        processDefinitionApi.delete(id, cascade);
        log.info("删除流程定义: " + id + ", 级联: " + cascade);
    }

    @Override
    public void suspendDefinition(String id) {
        processDefinitionApi.suspend(id);
        log.info("挂起流程定义: " + id);
    }

    @Override
    public void activateDefinition(String id) {
        processDefinitionApi.activate(id);
        log.info("激活流程定义: " + id);
    }

    @Override
    public List<ProcessDefinitionVO> getVersions(String key) {
        return processDefinitionApi.versions(key).stream().map(this::toVO).toList();
    }

    @Override
    public String getBpmnXml(String id) {
        return new String(processDefinitionApi.getBpmnXml(id), StandardCharsets.UTF_8);
    }

    @Override
    public ProcessDefinitionVO toVO(ProcessDefinitionInfo pd) {
        if (pd == null) {
            return null;
        }
        return ProcessDefinitionVO.builder()
                .id(pd.getId())
                .key(pd.getKey())
                .name(pd.getName())
                .version(pd.getVersion())
                .description(pd.getDescription())
                .category(pd.getCategory())
                .suspended(pd.getSuspended())
                .deploymentId(pd.getDeploymentId())
                .resourceName(pd.getResourceName())
                .diagramResourceName(pd.getDiagramResourceName())
                .tenantId(pd.getTenantId())
                .build();
    }
}
