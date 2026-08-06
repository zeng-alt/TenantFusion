package com.github.zeng.alt.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.entity.FormDataEntity;
import com.github.zeng.alt.workflow.entity.FormTemplateEntity;
import com.github.zeng.alt.workflow.entity.FormTemplateVersionEntity;
import com.github.zeng.alt.workflow.model.FormDataCreateCmd;
import com.github.zeng.alt.workflow.model.FormDataQuery;
import com.github.zeng.alt.workflow.model.FormDataStatus;
import com.github.zeng.alt.workflow.model.FormDataUpdateCmd;
import com.github.zeng.alt.workflow.model.FormDataVO;
import com.github.zeng.alt.workflow.model.FormTemplateVersionStatus;
import com.github.zeng.alt.workflow.repository.FormDataRepository;
import com.github.zeng.alt.workflow.repository.FormTemplateRepository;
import com.github.zeng.alt.workflow.repository.FormTemplateVersionRepository;
import com.github.zeng.alt.workflow.service.FormDataService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 动态表单数据服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class FormDataServiceImpl implements FormDataService {

    private final FormDataRepository formDataRepository;
    private final FormTemplateRepository formTemplateRepository;
    private final FormTemplateVersionRepository formTemplateVersionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageRestResponse<FormDataVO> page(FormDataQuery query) {
        Predicate predicate = buildPredicate(query);
        Sort sort = Sort.by(Sort.Direction.fromOptionalString(query.getOrder()).orElse(Sort.Direction.DESC),
                query.getSort());
        Page<FormDataEntity> pageResult = formDataRepository.findAll(predicate,
                PageRequest.of(query.getPage() - 1, query.getPageSize(), sort));
        List<FormDataEntity> entities = pageResult.getContent();
        Map<Long, String> templateNames = resolveTemplateNames(entities);
        List<FormDataVO> vos = entities.stream()
                .map(entity -> FormDataVO.from(entity, objectMapper,
                        templateNames.get(entity.getFormTemplateId()))).toList();
        return PageRestResponse.of(vos, pageResult.getTotalElements(), query.getPageSize(), query.getPage());
    }

    @Override
    @Transactional(readOnly = true)
    public FormDataVO getDetail(Long id) {
        FormDataEntity entity = getRequiredEntity(id);
        return FormDataVO.from(entity, objectMapper, resolveTemplateName(entity.getFormTemplateId()));
    }

    @Override
    @Transactional
    public FormDataVO create(FormDataCreateCmd cmd) {
        FormTemplateEntity template = formTemplateRepository.findById(cmd.getFormTemplateId())
                .getOrElseThrow(() -> new BaseException("表单模板不存在: " + cmd.getFormTemplateId()));
        FormDataEntity entity = new FormDataEntity();
        entity.setFormTemplateId(template.getFormTemplateId());
        entity.setFormVersion(resolveCurrentFormVersion(template.getFormTemplateId(), cmd.getFormVersion()));
        entity.setProcessInstanceId(cmd.getProcessInstanceId());
        entity.setData(serializeData(cmd.getData()));
        entity.setStatus(cmd.getStatus() == null ? FormDataStatus.SUBMITTED : cmd.getStatus());
        entity.setRemark(cmd.getRemark());
        if (entity.getStatus() == FormDataStatus.SUBMITTED) {
            entity.setSubmittedDate(LocalDateTime.now());
        }
        FormDataEntity saved = formDataRepository.save(entity);
        log.info("创建表单数据: formTemplateId=" + saved.getFormTemplateId() + ", id=" + saved.getFormDataId());
        return FormDataVO.from(saved, objectMapper, template.getName());
    }

    @Override
    @Transactional
    public FormDataVO update(Long id, FormDataUpdateCmd cmd) {
        FormDataEntity entity = getRequiredEntity(id);
        if (cmd.getFormVersion() != null) {
            entity.setFormVersion(cmd.getFormVersion());
        }
        if (cmd.getProcessInstanceId() != null) {
            entity.setProcessInstanceId(cmd.getProcessInstanceId());
        }
        if (cmd.getData() != null) {
            entity.setData(serializeData(cmd.getData()));
        }
        if (cmd.getStatus() != null && cmd.getStatus() != entity.getStatus()) {
            if (cmd.getStatus() == FormDataStatus.SUBMITTED && entity.getSubmittedDate() == null) {
                entity.setSubmittedDate(LocalDateTime.now());
            }
            entity.setStatus(cmd.getStatus());
        }
        if (cmd.getRemark() != null) {
            entity.setRemark(cmd.getRemark());
        }
        FormDataEntity saved = formDataRepository.save(entity);
        log.info("更新表单数据: id=" + id);
        return FormDataVO.from(saved, objectMapper, resolveTemplateName(saved.getFormTemplateId()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getRequiredEntity(id);
        formDataRepository.deleteById(id);
        log.info("删除表单数据: id=" + id);
    }

    private Predicate buildPredicate(FormDataQuery query) {
        BooleanBuilder builder = new BooleanBuilder();
        com.github.zeng.alt.workflow.entity.QFormDataEntity q =
                com.github.zeng.alt.workflow.entity.QFormDataEntity.formDataEntity;
        if (query.getFormTemplateId() != null) {
            builder.and(q.formTemplateId.eq(query.getFormTemplateId()));
        }
        if (StringUtils.hasText(query.getProcessInstanceId())) {
            builder.and(q.processInstanceId.containsIgnoreCase(query.getProcessInstanceId()));
        }
        if (query.getStatus() != null) {
            builder.and(q.status.eq(query.getStatus()));
        }
        return builder;
    }

    private FormDataEntity getRequiredEntity(Long id) {
        return formDataRepository.findById(id)
                .getOrElseThrow(() -> new BaseException("表单数据不存在: " + id));
    }

    /**
     * 批量解析表单模板名称（避免 N+1）
     *
     * @param entities 表单数据列表
     * @return 模板ID → 模板名称
     */
    private Map<Long, String> resolveTemplateNames(List<FormDataEntity> entities) {
        Set<Long> templateIds = entities.stream()
                .map(FormDataEntity::getFormTemplateId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        return formTemplateRepository.findByIdIn(templateIds).stream()
                .collect(Collectors.toMap(FormTemplateEntity::getFormTemplateId,
                        FormTemplateEntity::getName, (a, b) -> a));
    }

    /**
     * 解析单个表单模板名称
     *
     * @param formTemplateId 模板ID
     * @return 模板名称
     */
    private String resolveTemplateName(Long formTemplateId) {
        if (formTemplateId == null) {
            return null;
        }
        return formTemplateRepository.findById(formTemplateId)
                .map(FormTemplateEntity::getName).getOrNull();
    }

    /**
     * 解析提交时的表单版本快照：优先使用调用方指定版本，否则取模板当前生效的已发布版本号
     *
     * @param formTemplateId 模板ID
     * @param requested      调用方指定版本（可为空）
     * @return 版本号
     */
    private Integer resolveCurrentFormVersion(Long formTemplateId, Integer requested) {
        if (requested != null) {
            return requested;
        }
        return formTemplateVersionRepository
                .findFirstByFormTemplateIdAndCurrentTrue(formTemplateId)
                .map(FormTemplateVersionEntity::getVersion)
                .orElseGet(() -> formTemplateVersionRepository
                        .findFirstByFormTemplateIdAndStatusOrderByVersionDesc(formTemplateId, FormTemplateVersionStatus.PUBLISHED)
                        .map(FormTemplateVersionEntity::getVersion)
                        .orElse(1));
    }

    /**
     * 将字段值 JSON 序列化为字符串存储
     *
     * @param data 字段值（可能为 JSON 字符串）
     * @return 规范化后的 JSON 字符串
     */
    private String serializeData(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(data));
        } catch (JsonProcessingException e) {
            throw new BaseException("表单数据不是合法的 JSON: " + e.getOriginalMessage());
        }
    }
}