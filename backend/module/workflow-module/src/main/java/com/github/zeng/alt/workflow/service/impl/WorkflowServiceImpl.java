package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.entity.FormTemplateEntity;
import com.github.zeng.alt.workflow.entity.FormTemplateVersionEntity;
import com.github.zeng.alt.workflow.entity.WorkflowEntity;
import com.github.zeng.alt.workflow.entity.WorkflowVersionEntity;
import com.github.zeng.alt.workflow.mapper.WorkflowMapper;
import com.github.zeng.alt.workflow.mapper.WorkflowVersionMapper;
import com.github.zeng.alt.workflow.model.*;
import com.github.zeng.alt.workflow.repository.WorkflowRepository;
import com.github.zeng.alt.workflow.repository.WorkflowVersionRepository;
import com.github.zeng.alt.workflow.service.WorkflowService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程管理服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowMapper workflowMapper;
    private final WorkflowVersionMapper workflowVersionMapper;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;

    @Override
    @Transactional(readOnly = true)
    public PageRestResponse<WorkflowVO> page(WorkflowQuery query) {
        Predicate predicate = buildPredicate(query);
        Page<WorkflowEntity> pageResult = workflowRepository.findAll(predicate, query.toPage());
        List<WorkflowVO> vos = pageResult.getContent().stream().map(workflowMapper::toVO).toList();
        return PageRestResponse.of(vos, pageResult.getTotalElements(), query.getPageSize(), query.getPage());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowVO getDetail(Long id) {
        return workflowMapper.toVO(getRequiredEntity(id));
    }

    @Override
    @Transactional
    public WorkflowVO create(WorkflowCreateCmd cmd) {
        if (workflowRepository.existsByWorkflowKey(cmd.getWorkflowKey())) {
            throw new BaseException("流程编码已存在: " + cmd.getWorkflowKey());
        }
        WorkflowEntity entity = workflowMapper.toEntity(cmd);
        entity.setCurrentVersion(0);
        entity.setLatestVersion(1);
        WorkflowEntity saved = workflowRepository.save(entity);

        WorkflowVersionEntity draft = new WorkflowVersionEntity();
        draft.setWorkflowId(saved.getWorkflowId());
        draft.setVersion(1);
        draft.setStatus(WorkflowVersionStatus.DRAFT);
        draft.setCurrent(false);
        draft.setRemark(cmd.getRemark());
        workflowVersionRepository.save(draft);

        log.info("创建流程: " + saved.getWorkflowKey() + ", id=" + saved.getWorkflowId());
        return workflowMapper.toVO(saved);
    }

    @Override
    @Transactional
    public WorkflowVO update(Long id, WorkflowUpdateCmd cmd) {
        WorkflowEntity entity = getRequiredEntity(id);
        workflowMapper.merge(cmd, entity);
        WorkflowEntity saved = workflowRepository.save(entity);
        return workflowMapper.toVO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WorkflowEntity entity = getRequiredEntity(id);
        List<WorkflowVersionEntity> versions = workflowVersionRepository.findByWorkflowIdOrderByVersionDesc(id);
        boolean hasPublished = versions.stream().anyMatch(v -> v.getStatus() == WorkflowVersionStatus.PUBLISHED);
        if (hasPublished) {
            boolean hasInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(entity.getWorkflowKey())
                    .count() > 0;
            if (hasInstances) {
                throw new BaseException("流程 [ " + entity.getWorkflowKey() + " ] 存在运行中的实例，无法删除");
            }
            for (WorkflowVersionEntity version : versions) {
                if (StringUtils.hasText(version.getProcessDefinitionId())) {
                    repositoryService.deleteProcessDefinition(version.getProcessDefinitionId(), false);
                }
            }
        }
        workflowVersionRepository.deleteByWorkflowId(id);
        workflowRepository.deleteById(id);
        log.info("删除流程: " + entity.getWorkflowKey() + ", id=" + id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowVersionVO> versions(Long workflowId) {
        getRequiredEntity(workflowId);
        return workflowVersionRepository
                .findProjectByWorkflowIdOrderByVersionDesc(workflowId)
                .stream()
                .map(workflowVersionMapper::toVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowVersionVO getVersion(Long templateId, Integer version) {
        return workflowVersionRepository
                .findByWorkflowIdAndVersion(templateId, version)
                .map(workflowVersionMapper::toVO)
                .getOrElse((WorkflowVersionVO) null);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowVersionVO getVersion(Long versionId) throws IOException {
        WorkflowVersionVO vo = workflowVersionRepository
                .findById(versionId)
                .map(workflowVersionMapper::toVO)
                .getOrElse((WorkflowVersionVO) null);

        if (vo == null) {
            return null;
        }

        if (vo.getStatus() != WorkflowVersionStatus.DRAFT
                && StringUtils.hasText(vo.getProcessDefinitionId())) {
            vo.setBpmnXml(IOUtils.toString(repositoryService.getProcessModel(vo.getProcessDefinitionId()), Charset.defaultCharset()));
        }
        return vo;
    }

    private WorkflowVersionEntity saveDraftEntity(Long workflowId, WorkflowSaveDraftCmd cmd) {
        WorkflowEntity workflow = workflowRepository.findById(workflowId)
                .getOrElseThrow(() -> new BaseException("流程不存在: " + workflowId));

        WorkflowVersionEntity draft = workflowVersionRepository
                .findFirstByWorkflowIdAndStatusOrderByVersionDesc(workflow.getWorkflowId(), WorkflowVersionStatus.DRAFT)
                .orElseGet(() -> {
                    WorkflowVersionEntity created = new WorkflowVersionEntity();
                    created.setWorkflowId(workflow.getWorkflowId());
                    created.setVersion((workflow.getLatestVersion() == null ? 0 : workflow.getLatestVersion()) + 1);
                    created.setStatus(WorkflowVersionStatus.DRAFT);
                    created.setCurrent(false);
                    return created;
                });
        draft.setBpmnXml(cmd.getBpmnXml());
        WorkflowVersionEntity saved = workflowVersionRepository.save(draft);
        if (workflow.getLatestVersion() == null || workflow.getLatestVersion() < saved.getVersion()) {
            workflow.setLatestVersion(saved.getVersion());
            workflowRepository.save(workflow);
        }
        log.info("保存流程草稿: workflowId=" + workflow.getWorkflowId() + ", version=" + saved.getVersion());
        return draft;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowVersionVO saveDraft(Long workflowId, WorkflowSaveDraftCmd cmd) {
        return workflowVersionMapper.toVO(saveDraftEntity(workflowId, cmd));
    }


    @Override
    public WorkflowVersionVO saveDraftAndPublish(Long workflowId, WorkflowSaveDraftCmd cmd) {
        WorkflowVersionEntity draft = saveDraftEntity(workflowId, cmd);
        return workflowVersionMapper.toVO(doPublish(draft));
    }

    @Override
    @Transactional
    public WorkflowVersionVO publish(Long versionId) {
        WorkflowVersionEntity version = getRequiredVersion(versionId);
        if (version.getStatus() != WorkflowVersionStatus.DRAFT
                && version.getStatus() != WorkflowVersionStatus.OFFLINE) {
            throw new BaseException("仅草稿或已下线版本可上线，当前状态: " + version.getStatus());
        }
        return workflowVersionMapper.toVO(doPublish(version));
    }

    /**
     * 上线核心逻辑：部署到 Camunda 并置为已发布，发布后重算流程当前版本（最大已发布版本号）。
     * <p>
     * 已下线版本重上线时本地 BPMN 已清空，从 Camunda 原部署模型中读取后重新部署。
     *
     * @param version 待上线的版本实体
     * @return 上线后的版本实体
     */
    private WorkflowVersionEntity doPublish(WorkflowVersionEntity version) {
        WorkflowEntity workflow = getRequiredEntity(version.getWorkflowId());
        String bpmnXml = version.getBpmnXml();
        if (!StringUtils.hasText(bpmnXml) && StringUtils.hasText(version.getProcessDefinitionId())) {
            try {
                bpmnXml = IOUtils.toString(
                        repositoryService.getProcessModel(version.getProcessDefinitionId()), Charset.defaultCharset());
            } catch (IOException e) {
                throw new BaseException("读取已下线版本 BPMN 失败: " + e.getMessage(), e);
            }
        }
        if (!StringUtils.hasText(bpmnXml)) {
            throw new BaseException("版本内容为空，请先保存流程设计");
        }

        DeploymentBuilder builder = repositoryService.createDeployment()
                .name(workflow.getWorkflowName())
                .addString(workflow.getWorkflowKey() + ".bpmn", bpmnXml);
        Deployment deployment;
        try {
            deployment = builder.deploy();
        } catch (RuntimeException e) {
            throw new BaseException("流程部署失败: " + e.getMessage(), e);
        }
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        if (pd == null) {
            repositoryService.deleteDeployment(deployment.getId(), true);
            throw new BaseException("部署成功但未找到流程定义");
        }

        version.setStatus(WorkflowVersionStatus.PUBLISHED);
        version.setCurrent(true);
        version.setBpmnXml(null);
        version.setDeploymentId(deployment.getId());
        version.setProcessDefinitionId(pd.getId());
        version.setPublishedDate(LocalDateTime.now());
        WorkflowVersionEntity saved = workflowVersionRepository.save(version);
        syncCurrentVersion(workflow);
        log.info("上线流程版本: workflowId=" + workflow.getWorkflowId() + ", version=" + saved.getVersion()
                + ", deploymentId=" + deployment.getId());
        return saved;
    }

    @Override
    @Transactional
    public WorkflowVersionVO offline(Long versionId) {
        WorkflowVersionEntity version = getRequiredVersion(versionId);
        if (version.getStatus() != WorkflowVersionStatus.PUBLISHED) {
            throw new BaseException("仅已发布版本可下线，当前状态: " + version.getStatus());
        }
        boolean wasCurrent = Boolean.TRUE.equals(version.getCurrent());
        if (wasCurrent && StringUtils.hasText(version.getProcessDefinitionId())) {
            repositoryService.suspendProcessDefinitionById(version.getProcessDefinitionId(), true, null);
        }
        version.setStatus(WorkflowVersionStatus.OFFLINE);
        version.setCurrent(false);
        WorkflowVersionEntity saved = workflowVersionRepository.save(version);
        syncCurrentVersion(getRequiredEntity(saved.getWorkflowId()));
        log.info("下线流程版本: versionId=" + versionId + ", version=" + saved.getVersion());
        return workflowVersionMapper.toVO(saved);
    }

    /**
     * 重算流程当前版本号：取状态为「已发布」且版本号最大的版本；
     * 没有已发布版本时置为 0，并同步各版本的 current 标记。
     *
     * @param workflow 流程
     */
    private void syncCurrentVersion(WorkflowEntity workflow) {
        Integer current = workflowVersionRepository
                .findByWorkflowIdAndStatus(workflow.getWorkflowId(), WorkflowVersionStatus.PUBLISHED)
                .stream()
                .map(WorkflowVersionEntity::getVersion)
                .max(Integer::compareTo)
                .orElse(0);
        workflow.setCurrentVersion(current);
        workflowRepository.save(workflow);
        workflowVersionRepository.findByWorkflowIdOrderByVersionDesc(workflow.getWorkflowId())
                .forEach(version -> {
                    boolean isCurrent = current > 0
                            && version.getStatus() == WorkflowVersionStatus.PUBLISHED
                            && version.getVersion().equals(current);
                    if (!Boolean.valueOf(isCurrent).equals(version.getCurrent())) {
                        version.setCurrent(isCurrent);
                        workflowVersionRepository.save(version);
                    }
                });
    }

    private void suspendOrActivate(WorkflowEntity workflow, boolean suspend) {
        WorkflowVersionEntity current = workflowVersionRepository
                .findFirstByWorkflowIdAndCurrentTrue(workflow.getWorkflowId())
                .orElseThrow(() -> new BaseException("流程 [ " + workflow.getWorkflowKey() + " ] 没有已发布的版本"));
        if (!StringUtils.hasText(current.getProcessDefinitionId())) {
            throw new BaseException("当前版本未关联 Camunda 流程定义");
        }
        if (suspend) {
            repositoryService.suspendProcessDefinitionById(current.getProcessDefinitionId(), true, null);
        } else {
            repositoryService.activateProcessDefinitionById(current.getProcessDefinitionId(), true, null);
        }
        log.info((suspend ? "挂起" : "激活") + "流程: " + workflow.getWorkflowKey() + ", id=" + workflow.getWorkflowId());
    }

    private Predicate buildPredicate(WorkflowQuery query) {
        BooleanBuilder builder = new BooleanBuilder();
        com.github.zeng.alt.workflow.entity.QWorkflowEntity q = com.github.zeng.alt.workflow.entity.QWorkflowEntity.workflowEntity;
        if (StringUtils.hasText(query.getWorkflowKey())) {
            builder.and(q.workflowKey.containsIgnoreCase(query.getWorkflowKey()));
        }
        if (StringUtils.hasText(query.getWorkflowName())) {
            builder.and(q.workflowName.containsIgnoreCase(query.getWorkflowName()));
        }
        if (StringUtils.hasText(query.getCategory())) {
            builder.and(q.category.containsIgnoreCase(query.getCategory()));
        }
        return builder;
    }

    private WorkflowEntity getRequiredEntity(Long id) {
        return workflowRepository.findById(id)
                .getOrElseThrow(() -> new BaseException("流程不存在: " + id));
    }

    private WorkflowVersionEntity getRequiredVersion(Long versionId) {
        return workflowVersionRepository.findById(versionId)
                .getOrElseThrow(() -> new BaseException("流程版本不存在: " + versionId));
    }
}