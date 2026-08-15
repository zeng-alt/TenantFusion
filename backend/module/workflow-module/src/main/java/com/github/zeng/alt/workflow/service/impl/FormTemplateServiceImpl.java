package com.github.zeng.alt.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.form.schema.DslFormSchemaConverter;
import com.github.zeng.alt.workflow.entity.FormTemplateEntity;
import com.github.zeng.alt.workflow.entity.FormTemplateVersionEntity;
import com.github.zeng.alt.workflow.model.FormTemplateCreateCmd;
import com.github.zeng.alt.workflow.model.FormTemplateQuery;
import com.github.zeng.alt.workflow.model.FormTemplateSaveDraftCmd;
import com.github.zeng.alt.workflow.model.FormTemplateUpdateCmd;
import com.github.zeng.alt.workflow.mapper.FormTemplateMapper;
import com.github.zeng.alt.workflow.mapper.FormTemplateVersionMapper;
import com.github.zeng.alt.workflow.model.FormTemplatePublishedVO;
import com.github.zeng.alt.workflow.model.FormTemplateVO;
import com.github.zeng.alt.workflow.model.FormTemplateVersionStatus;
import com.github.zeng.alt.workflow.model.FormTemplateVersionVO;
import com.github.zeng.alt.workflow.repository.FormTemplateRepository;
import com.github.zeng.alt.workflow.repository.FormTemplateVersionRepository;
import com.github.zeng.alt.workflow.service.FormTemplateService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态表单模板服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class FormTemplateServiceImpl implements FormTemplateService {

    private final FormTemplateRepository formTemplateRepository;
    private final FormTemplateVersionRepository formTemplateVersionRepository;
    private final FormTemplateMapper formTemplateMapper;
    private final FormTemplateVersionMapper formTemplateVersionMapper;
    private final DslFormSchemaConverter dslFormSchemaConverter;
    private final ObjectMapper objectMapper;
    @Override
    @Transactional(readOnly = true)
    public PageRestResponse<FormTemplateVO> page(FormTemplateQuery query) {
        Predicate predicate = buildPredicate(query);
        Page<FormTemplateEntity> pageResult = formTemplateRepository.findAll(predicate, query.toPage());
        List<FormTemplateVO> vos = pageResult.getContent().stream().map(formTemplateMapper::toVO).toList();
        return PageRestResponse.of(vos, pageResult.getTotalElements(), query.getPageSize(), query.getPageNo());
    }

    @Override
    @Transactional(readOnly = true)
    public FormTemplateVO getDetail(Long id) {
        return formTemplateMapper.toVO(getRequiredEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public FormTemplatePublishedVO getPublishedByCode(String code) {
        FormTemplateEntity template = formTemplateRepository.findByCode(code)
                .orElseThrow(() -> new BaseException("表单模板不存在: " + code));
        FormTemplateVersionEntity current = formTemplateVersionRepository
                .findFirstByFormTemplateIdAndCurrentTrue(template.getFormTemplateId())
                .orElseGet(() -> formTemplateVersionRepository
                        .findFirstByFormTemplateIdAndStatusOrderByVersionDesc(
                                template.getFormTemplateId(), FormTemplateVersionStatus.PUBLISHED)
                        .orElseThrow(() -> new BaseException("表单模板未发布: " + code)));
        FormTemplatePublishedVO vo = new FormTemplatePublishedVO();
        vo.setFormTemplateId(template.getFormTemplateId());
        vo.setName(template.getName());
        vo.setCode(template.getCode());
        vo.setCurrentVersion(template.getCurrentVersion());
        vo.setVersion(current.getVersion());
        vo.setDefinition(parseDefinition(current.getDefinition()));
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public JsonNode getSchemaByCode(String code) {
        return dslFormSchemaConverter.convert(getPublishedByCode(code).getDefinition());
    }

    @Override
    @Transactional(readOnly = true)
    public JsonNode getSchemaByVersion(Long versionId) {
        FormTemplateVersionEntity version = formTemplateVersionRepository.findById(versionId)
                .getOrElseThrow(() -> new BaseException("表单模板版本不存在: " + versionId));
        return dslFormSchemaConverter.convert(parseDefinition(version.getDefinition()));
    }

    @Override
    @Transactional
    public FormTemplateVO create(FormTemplateCreateCmd cmd) {
        if (formTemplateRepository.existsByCode(cmd.getCode())) {
            throw new BaseException("模板编码已存在: " + cmd.getCode());
        }
        FormTemplateEntity entity = formTemplateMapper.toEntity(cmd);
        entity.setCurrentVersion(0);
        entity.setLatestVersion(1);
        FormTemplateEntity saved = formTemplateRepository.save(entity);

        FormTemplateVersionEntity draft = new FormTemplateVersionEntity();
        draft.setFormTemplateId(saved.getFormTemplateId());
        draft.setStatus(FormTemplateVersionStatus.DRAFT);
        draft.setCurrent(false);
        draft.setVersion(1);
        draft.setRemark(cmd.getRemark());
        formTemplateVersionRepository.save(draft);
        log.info("创建表单模板: " + saved.getCode() + ", id=" + saved.getFormTemplateId());
        return formTemplateMapper.toVO(saved);
    }

    @Override
    @Transactional
    public FormTemplateVO update(Long id, FormTemplateUpdateCmd cmd) {
        FormTemplateEntity entity = getRequiredEntity(id);
        formTemplateMapper.merge(cmd, entity);
        FormTemplateEntity saved = formTemplateRepository.save(entity);
        log.info("更新表单模板: " + saved.getCode() + ", id=" + id);
        return formTemplateMapper.toVO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getRequiredEntity(id);
        formTemplateVersionRepository.deleteByFormTemplateId(id);
        formTemplateRepository.deleteById(id);
        log.info("删除表单模板: id=" + id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormTemplateVersionVO> versions(Long formTemplateId) {
        getRequiredEntity(formTemplateId);
        return formTemplateVersionRepository
                .findProjectionByFormTemplateIdOrderByVersionDesc(formTemplateId)
                .stream()
                .map(formTemplateVersionMapper::toVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FormTemplateVersionVO getVersion(Long templateId, Integer version) {
        return formTemplateVersionRepository
                .findByFormTemplateIdAndVersion(templateId, version)
                .map(formTemplateVersionMapper::toVO)
                .getOrElse((FormTemplateVersionVO) null);
    }

    @Override
    public FormTemplateVersionVO getVersion(Long versionId) {
        return formTemplateVersionRepository
                .findById(versionId)
                .map(formTemplateVersionMapper::toVO)
                .getOrElse((FormTemplateVersionVO) null);
    }

    @Override
    @Transactional
    public FormTemplateVersionVO saveDraft(Long formTemplateId, FormTemplateSaveDraftCmd cmd) {
        return formTemplateVersionMapper.toVO(saveDraftEntity(formTemplateId, cmd));
    }

    /**
     * 保存草稿的核心逻辑，返回保存后的版本实体（供 saveDraft / saveAndPublish 复用）
     */
    private FormTemplateVersionEntity saveDraftEntity(Long formTemplateId, FormTemplateSaveDraftCmd cmd) {
        FormTemplateEntity template = formTemplateRepository.findById(formTemplateId)
                        .getOrElseThrow(() -> new BaseException("表单模板不存在: " + formTemplateId));
        FormTemplateVersionEntity draft = formTemplateVersionRepository
                .findFirstByFormTemplateIdAndStatusOrderByVersionDesc(template.getFormTemplateId(), FormTemplateVersionStatus.DRAFT)
                .orElseGet(() -> {
                    FormTemplateVersionEntity created = new FormTemplateVersionEntity();
                    created.setFormTemplateId(template.getFormTemplateId());
                    created.setVersion((template.getLatestVersion() == null ? 0 : template.getLatestVersion()) + 1);
                    created.setStatus(FormTemplateVersionStatus.DRAFT);
                    created.setCurrent(false);
                    return created;
                });
        if (cmd.getDefinition() == null || cmd.getDefinition().isBlank()) {
            throw new BaseException("表单定义不能为空");
        }
        draft.setDefinition(serializeDefinition(cmd.getDefinition()));
        FormTemplateVersionEntity saved = formTemplateVersionRepository.save(draft);
        if (template.getLatestVersion() == null || template.getLatestVersion() < saved.getVersion()) {
            template.setLatestVersion(saved.getVersion());
            formTemplateRepository.save(template);
        }
        log.info("保存表单模板草稿: formTemplateId=" + template.getFormTemplateId() + ", version=" + saved.getVersion());
        return saved;
    }

    @Override
    @Transactional
    public FormTemplateVersionVO saveAndPublish(Long formTemplateId, FormTemplateSaveDraftCmd cmd) {
        FormTemplateVersionEntity draft = saveDraftEntity(formTemplateId, cmd);
        return formTemplateVersionMapper.toVO(doPublish(draft));
    }

    @Override
    @Transactional
    public FormTemplateVersionVO publish(Long versionId) {
        FormTemplateVersionEntity version = getRequiredVersion(versionId);
        if (version.getStatus() != FormTemplateVersionStatus.DRAFT
                && version.getStatus() != FormTemplateVersionStatus.OFFLINE) {
            throw new BaseException("仅草稿或已下线版本可上线，当前状态: " + version.getStatus());
        }
        if (!StringUtils.hasText(version.getDefinition())) {
            throw new BaseException("版本内容为空，请先保存表单设计");
        }
        return formTemplateVersionMapper.toVO(doPublish(version));
    }

    /**
     * 发布核心逻辑：将版本置为已发布并生效（供 saveAndPublish / publish 复用），
     * 发布后重算模板当前版本（最大已发布版本号）。
     *
     * @param version 待发布的版本实体
     * @return 发布后的版本实体
     */
    private FormTemplateVersionEntity doPublish(FormTemplateVersionEntity version) {
        version.setStatus(FormTemplateVersionStatus.PUBLISHED);
        version.setCurrent(true);
        version.setPublishedDate(LocalDateTime.now());
        FormTemplateVersionEntity saved = formTemplateVersionRepository.save(version);
        syncCurrentVersion(getRequiredEntity(saved.getFormTemplateId()));
        log.info("上线表单模板版本: formTemplateId=" + saved.getFormTemplateId() + ", version=" + saved.getVersion());
        return saved;
    }

    @Override
    @Transactional
    public FormTemplateVersionVO offline(Long versionId) {
        FormTemplateVersionEntity version = getRequiredVersion(versionId);
        if (version.getStatus() != FormTemplateVersionStatus.PUBLISHED) {
            throw new BaseException("仅已发布版本可下线，当前状态: " + version.getStatus());
        }
        version.setStatus(FormTemplateVersionStatus.OFFLINE);
        version.setCurrent(false);
        FormTemplateVersionEntity saved = formTemplateVersionRepository.save(version);
        syncCurrentVersion(getRequiredEntity(saved.getFormTemplateId()));
        log.info("下线表单模板版本: versionId=" + versionId + ", version=" + saved.getVersion());
        return formTemplateVersionMapper.toVO(saved);
    }

    /**
     * 重算模板当前版本号：取状态为「已发布」且版本号最大的版本；
     * 没有已发布版本时置为 0，并同步各版本的 current 标记。
     *
     * @param template 表单模板
     */
    private void syncCurrentVersion(FormTemplateEntity template) {
        Integer current = formTemplateVersionRepository
                .findByFormTemplateIdAndStatus(template.getFormTemplateId(), FormTemplateVersionStatus.PUBLISHED)
                .stream()
                .map(FormTemplateVersionEntity::getVersion)
                .max(Integer::compareTo)
                .orElse(0);
        template.setCurrentVersion(current);
        formTemplateRepository.save(template);
        formTemplateVersionRepository.findByFormTemplateIdOrderByVersionDesc(template.getFormTemplateId())
                .forEach(version -> {
                    boolean isCurrent = current > 0
                            && version.getStatus() == FormTemplateVersionStatus.PUBLISHED
                            && version.getVersion().equals(current);
                    if (!Boolean.valueOf(isCurrent).equals(version.getCurrent())) {
                        version.setCurrent(isCurrent);
                        formTemplateVersionRepository.save(version);
                    }
                });
    }

    private Predicate buildPredicate(FormTemplateQuery query) {
        BooleanBuilder builder = new BooleanBuilder();
        com.github.zeng.alt.workflow.entity.QFormTemplateEntity q =
                com.github.zeng.alt.workflow.entity.QFormTemplateEntity.formTemplateEntity;
        if (StringUtils.hasText(query.getName())) {
            builder.and(q.name.containsIgnoreCase(query.getName()));
        }
        if (StringUtils.hasText(query.getCode())) {
            builder.and(q.code.containsIgnoreCase(query.getCode()));
        }
        if (StringUtils.hasText(query.getCategory())) {
            builder.and(q.category.containsIgnoreCase(query.getCategory()));
        }
        return builder;
    }

    private FormTemplateEntity getRequiredEntity(Long id) {
        return formTemplateRepository.findById(id)
                .getOrElseThrow(() -> new BaseException("表单模板不存在: " + id));
    }

    private FormTemplateVersionEntity getRequiredVersion(Long versionId) {
        return formTemplateVersionRepository.findById(versionId)
                .getOrElseThrow(() -> new BaseException("表单模板版本不存在: " + versionId));
    }

    /**
     * 将定义 JSON 字符串解析为 JsonNode；空或非法 JSON 返回 null
     *
     * @param definition 定义（JSON 字符串）
     * @return 定义对象
     */
    private JsonNode parseDefinition(String definition) {
        if (definition == null || definition.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(definition);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 将定义 JSON 序列化为字符串存储；空串或非法 JSON 抛出业务异常
     *
     * @param definition 定义（JSON 字符串）
     * @return 规范化后的 JSON 字符串
     */
    private String serializeDefinition(String definition) {
        if (definition == null || definition.isBlank()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(definition));
        } catch (JsonProcessingException e) {
            throw new BaseException("表单定义不是合法的 JSON: " + e.getOriginalMessage());
        }
    }
}