package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.entity.FormConfigEntity;
import com.github.zeng.alt.workflow.entity.FormConfigVersionEntity;
import com.github.zeng.alt.workflow.entity.FormFieldEntity;
import com.github.zeng.alt.workflow.entity.FormFieldOptionEntity;
import com.github.zeng.alt.workflow.mapper.FormConfigMapper;
import com.github.zeng.alt.workflow.mapper.FormConfigVersionMapper;
import com.github.zeng.alt.workflow.model.FormConfigCreateCmd;
import com.github.zeng.alt.workflow.model.FormConfigOptionVO;
import com.github.zeng.alt.workflow.model.FormConfigQuery;
import com.github.zeng.alt.workflow.model.FormConfigSaveDraftCmd;
import com.github.zeng.alt.workflow.model.FormConfigUpdateCmd;
import com.github.zeng.alt.workflow.model.FormConfigVO;
import com.github.zeng.alt.workflow.model.FormConfigVersionStatus;
import com.github.zeng.alt.workflow.model.FormConfigVersionVO;
import com.github.zeng.alt.workflow.model.FormFieldOptionVO;
import com.github.zeng.alt.workflow.model.FormFieldVO;
import com.github.zeng.alt.workflow.repository.FormConfigRepository;
import com.github.zeng.alt.workflow.repository.FormConfigVersionListProjection;
import com.github.zeng.alt.workflow.repository.FormConfigVersionRepository;
import com.github.zeng.alt.workflow.repository.FormFieldOptionRepository;
import com.github.zeng.alt.workflow.repository.FormFieldRepository;
import com.github.zeng.alt.workflow.service.FormConfigService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置表单服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class FormConfigServiceImpl implements FormConfigService {

    private final FormConfigRepository formConfigRepository;
    private final FormConfigVersionRepository formConfigVersionRepository;
    private final FormFieldRepository formFieldRepository;
    private final FormFieldOptionRepository formFieldOptionRepository;
    private final FormConfigMapper formConfigMapper;
    private final FormConfigVersionMapper formConfigVersionMapper;

    @Override
    @Transactional(readOnly = true)
    public PageRestResponse<FormConfigVO> page(FormConfigQuery query) {
        Predicate predicate = buildPredicate(query);
        Page<FormConfigEntity> pageResult = formConfigRepository.findAll(predicate, query.toPage());
        List<FormConfigVO> vos = pageResult.getContent().stream().map(formConfigMapper::toVO).toList();
        return PageRestResponse.of(vos, pageResult.getTotalElements(), query.getPageSize(), query.getPageNo());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormConfigOptionVO> options() {
        return formConfigRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(FormConfigEntity::getCode))
                .map(entity -> {
                    FormConfigOptionVO vo = new FormConfigOptionVO();
                    vo.setFormConfigId(entity.getFormConfigId());
                    vo.setName(entity.getName());
                    vo.setCode(entity.getCode());
                    return vo;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FormConfigVO getDetail(Long id) {
        return formConfigMapper.toVO(getRequiredEntity(id));
    }

    @Override
    @Transactional
    public FormConfigVO create(FormConfigCreateCmd cmd) {
        if (formConfigRepository.existsByCode(cmd.getCode())) {
            throw new BaseException("表单编码已存在: " + cmd.getCode());
        }
        FormConfigEntity entity = formConfigMapper.toEntity(cmd);
        entity.setCurrentVersion(0);
        entity.setLatestVersion(1);
        FormConfigEntity saved = formConfigRepository.save(entity);

        FormConfigVersionEntity draft = new FormConfigVersionEntity();
        draft.setFormConfigId(saved.getFormConfigId());
        draft.setStatus(FormConfigVersionStatus.DRAFT);
        draft.setCurrent(false);
        draft.setVersion(1);
        draft.setRemark(cmd.getRemark());
        formConfigVersionRepository.save(draft);
        log.info("创建配置表单: " + saved.getCode() + ", id=" + saved.getFormConfigId());
        return formConfigMapper.toVO(saved);
    }

    @Override
    @Transactional
    public FormConfigVO update(Long id, FormConfigUpdateCmd cmd) {
        FormConfigEntity entity = getRequiredEntity(id);
        formConfigMapper.merge(cmd, entity);
        FormConfigEntity saved = formConfigRepository.save(entity);
        log.info("更新配置表单: " + saved.getCode() + ", id=" + id);
        return formConfigMapper.toVO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getRequiredEntity(id);
        List<FormConfigVersionEntity> versions = formConfigVersionRepository.findByFormConfigIdOrderByVersionDesc(id);
        for (FormConfigVersionEntity version : versions) {
            deleteFieldsByVersionId(version.getVersionId());
        }
        formConfigVersionRepository.deleteByFormConfigId(id);
        formConfigRepository.deleteById(id);
        log.info("删除配置表单: id=" + id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormConfigVersionVO> versions(Long formConfigId) {
        getRequiredEntity(formConfigId);
        List<FormConfigVersionVO> vos = new ArrayList<>();
        List<FormConfigVersionListProjection> projections = formConfigVersionRepository
                .findProjectionByFormConfigIdOrderByVersionDesc(formConfigId);
        for (FormConfigVersionListProjection projection : projections) {
            FormConfigVersionVO vo = formConfigVersionMapper.toVO(projection);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    @Transactional(readOnly = true)
    public FormConfigVersionVO getVersion(Long formConfigId, Integer version) {
        return formConfigVersionRepository
                .findByFormConfigIdAndVersion(formConfigId, version)
                .map(projection -> {
                    FormConfigVersionVO vo = formConfigVersionMapper.toVO(projection);
                    vo.setFields(buildFieldTree(projection.getVersionId()));
                    return vo;
                })
                .getOrElse((FormConfigVersionVO) null);
    }

    @Override
    @Transactional(readOnly = true)
    public FormConfigVersionVO getVersion(Long versionId) {
        return formConfigVersionRepository.findById(versionId)
                .map(entity -> {
                    FormConfigVersionVO vo = formConfigVersionMapper.toVO(entity);
                    vo.setFields(buildFieldTree(versionId));
                    return vo;
                })
                .getOrElse((FormConfigVersionVO) null);
    }

    @Override
    @Transactional
    public FormConfigVersionVO saveDraft(Long formConfigId, FormConfigSaveDraftCmd cmd) {
        FormConfigVersionEntity draft = saveDraftEntity(formConfigId, cmd);
        FormConfigVersionVO vo = formConfigVersionMapper.toVO(draft);
        vo.setFields(cmd.getFields());
        return vo;
    }

    @Override
    @Transactional
    public FormConfigVersionVO saveAndPublish(Long formConfigId, FormConfigSaveDraftCmd cmd) {
        FormConfigVersionEntity draft = saveDraftEntity(formConfigId, cmd);
        return doPublishVo(draft);
    }

    @Override
    @Transactional
    public FormConfigVersionVO publish(Long versionId) {
        FormConfigVersionEntity version = getRequiredVersion(versionId);
        if (version.getStatus() != FormConfigVersionStatus.DRAFT
                && version.getStatus() != FormConfigVersionStatus.OFFLINE) {
            throw new BaseException("仅草稿或已下线版本可上线，当前状态: " + version.getStatus());
        }
        return doPublishVo(version);
    }

    @Override
    @Transactional
    public FormConfigVersionVO offline(Long versionId) {
        FormConfigVersionEntity version = getRequiredVersion(versionId);
        if (version.getStatus() != FormConfigVersionStatus.PUBLISHED) {
            throw new BaseException("仅已发布版本可下线，当前状态: " + version.getStatus());
        }
        version.setStatus(FormConfigVersionStatus.OFFLINE);
        version.setCurrent(false);
        FormConfigVersionEntity saved = formConfigVersionRepository.save(version);
        syncCurrentVersion(getRequiredEntity(saved.getFormConfigId()));
        log.info("下线配置表单版本: versionId=" + versionId + ", version=" + saved.getVersion());
        FormConfigVersionVO vo = formConfigVersionMapper.toVO(saved);
        vo.setFields(buildFieldTree(saved.getVersionId()));
        return vo;
    }

    private FormConfigVersionEntity saveDraftEntity(Long formConfigId, FormConfigSaveDraftCmd cmd) {
        FormConfigEntity config = formConfigRepository.findById(formConfigId)
                .getOrElseThrow(() -> new BaseException("配置表单不存在: " + formConfigId));
        FormConfigVersionEntity draft = formConfigVersionRepository
                .findFirstByFormConfigIdAndStatusOrderByVersionDesc(config.getFormConfigId(), FormConfigVersionStatus.DRAFT)
                .orElseGet(() -> {
                    FormConfigVersionEntity created = new FormConfigVersionEntity();
                    created.setFormConfigId(config.getFormConfigId());
                    created.setVersion((config.getLatestVersion() == null ? 0 : config.getLatestVersion()) + 1);
                    created.setStatus(FormConfigVersionStatus.DRAFT);
                    created.setCurrent(false);
                    return created;
                });
        FormConfigVersionEntity saved = formConfigVersionRepository.save(draft);
        syncFields(saved.getVersionId(), cmd.getFields());
        saved.setLabelWidth(cmd.getLabelWidth());
        saved.setLabelPlacement(cmd.getLabelPlacement());
        saved.setLabelAlign(cmd.getLabelAlign());
        saved.setFormSize(cmd.getFormSize());
        formConfigVersionRepository.save(saved);
        if (config.getLatestVersion() == null || config.getLatestVersion() < saved.getVersion()) {
            config.setLatestVersion(saved.getVersion());
            formConfigRepository.save(config);
        }
        log.info("保存配置表单草稿: formConfigId=" + config.getFormConfigId() + ", version=" + saved.getVersion());
        return saved;
    }

    /**
     * 全量同步字段：删除旧字段，保存新字段树
     */
    private void syncFields(Long versionId, List<FormFieldVO> fieldVos) {
        deleteFieldsByVersionId(versionId);
        if (fieldVos != null) {
            saveFieldTree(versionId, null, fieldVos);
        }
    }

    private void saveFieldTree(Long versionId, Long parentFieldId, List<FormFieldVO> fieldVos) {
        if (fieldVos == null)
            return;
        for (int i = 0; i < fieldVos.size(); i++) {
            FormFieldVO vo = fieldVos.get(i);
            FormFieldEntity entity = new FormFieldEntity();
            entity.setVersionId(versionId);
            entity.setParentFieldId(parentFieldId);
            entity.setFieldKey(vo.getFieldKey());
            entity.setFieldLabel(vo.getFieldLabel());
            entity.setFieldType(vo.getFieldType());
            entity.setDefaultValue(vo.getDefaultValue());
            entity.setPlaceholder(vo.getPlaceholder());
            entity.setHelpText(vo.getHelpText());
            entity.setSortOrder(vo.getSortOrder() != null ? vo.getSortOrder() : i);
            entity.setColSpan(vo.getColSpan());
            entity.setRequired(vo.getRequired());
            entity.setReadonly(vo.getReadonly());
            entity.setHidden(vo.getHidden());
            entity.setValidationRules(vo.getValidationRules());
            entity.setVisibilityCondition(vo.getVisibilityCondition());
            entity.setFieldProps(vo.getFieldProps());
            FormFieldEntity saved = formFieldRepository.save(entity);

            if (vo.getOptions() != null) {
                for (int j = 0; j < vo.getOptions().size(); j++) {
                    FormFieldOptionVO optVo = vo.getOptions().get(j);
                    FormFieldOptionEntity opt = new FormFieldOptionEntity();
                    opt.setFieldId(saved.getFieldId());
                    opt.setLabel(optVo.getLabel());
                    opt.setValue(optVo.getValue());
                    opt.setSortOrder(optVo.getSortOrder() != null ? optVo.getSortOrder() : j);
                    formFieldOptionRepository.save(opt);
                }
            }

            if (vo.getChildren() != null) {
                saveFieldTree(versionId, saved.getFieldId(), vo.getChildren());
            }
        }
    }

    private void deleteFieldsByVersionId(Long versionId) {
        List<FormFieldEntity> fields = formFieldRepository.findByVersionIdOrderBySortOrderAsc(versionId);
        for (FormFieldEntity field : fields) {
            formFieldOptionRepository.deleteByFieldId(field.getFieldId());
            formFieldRepository.deleteById(field.getFieldId());
        }
    }

    /**
     * 从数据库读取字段并构建树形结构
     */
    private List<FormFieldVO> buildFieldTree(Long versionId) {
        List<FormFieldEntity> allFields = formFieldRepository.findByVersionIdOrderBySortOrderAsc(versionId);
        if (allFields.isEmpty())
            return Collections.emptyList();

        Map<Long, FormFieldVO> voMap = new HashMap<>();
        Map<Long, List<FormFieldEntity>> childrenMap = new HashMap<>();
        List<FormFieldEntity> roots = new ArrayList<>();

        for (FormFieldEntity entity : allFields) {
            FormFieldVO vo = entityToVO(entity);
            voMap.put(entity.getFieldId(), vo);

            if (entity.getParentFieldId() == null) {
                roots.add(entity);
            } else {
                childrenMap.computeIfAbsent(entity.getParentFieldId(), k -> new ArrayList<>()).add(entity);
            }
        }

        for (Map.Entry<Long, List<FormFieldEntity>> entry : childrenMap.entrySet()) {
            FormFieldVO parent = voMap.get(entry.getKey());
            if (parent != null) {
                List<FormFieldVO> childVos = entry.getValue().stream()
                        .map(e -> voMap.get(e.getFieldId()))
                        .toList();
                parent.setChildren(childVos);
            }
        }

        return roots.stream().map(e -> voMap.get(e.getFieldId())).toList();
    }

    private FormFieldVO entityToVO(FormFieldEntity entity) {
        FormFieldVO vo = new FormFieldVO();
        vo.setFieldId(entity.getFieldId());
        vo.setParentFieldId(entity.getParentFieldId());
        vo.setFieldKey(entity.getFieldKey());
        vo.setFieldLabel(entity.getFieldLabel());
        vo.setFieldType(entity.getFieldType());
        vo.setDefaultValue(entity.getDefaultValue());
        vo.setPlaceholder(entity.getPlaceholder());
        vo.setHelpText(entity.getHelpText());
        vo.setSortOrder(entity.getSortOrder());
        vo.setColSpan(entity.getColSpan());
        vo.setRequired(entity.getRequired());
        vo.setReadonly(entity.getReadonly());
        vo.setHidden(entity.getHidden());
        vo.setValidationRules(entity.getValidationRules());
        vo.setVisibilityCondition(entity.getVisibilityCondition());
        vo.setFieldProps(entity.getFieldProps());

        List<FormFieldOptionEntity> opts = formFieldOptionRepository.findByFieldIdOrderBySortOrderAsc(entity.getFieldId());
        if (!opts.isEmpty()) {
            List<FormFieldOptionVO> optVos = opts.stream().map(opt -> {
                FormFieldOptionVO ovo = new FormFieldOptionVO();
                ovo.setOptionId(opt.getOptionId());
                ovo.setLabel(opt.getLabel());
                ovo.setValue(opt.getValue());
                ovo.setSortOrder(opt.getSortOrder());
                return ovo;
            }).toList();
            vo.setOptions(optVos);
        }
        return vo;
    }

    private FormConfigVersionVO doPublishVo(FormConfigVersionEntity version) {
        version.setStatus(FormConfigVersionStatus.PUBLISHED);
        version.setCurrent(true);
        version.setPublishedDate(LocalDateTime.now());
        FormConfigVersionEntity saved = formConfigVersionRepository.save(version);
        syncCurrentVersion(getRequiredEntity(saved.getFormConfigId()));
        log.info("上线配置表单版本: formConfigId=" + saved.getFormConfigId() + ", version=" + saved.getVersion());
        FormConfigVersionVO vo = formConfigVersionMapper.toVO(saved);
        vo.setFields(buildFieldTree(saved.getVersionId()));
        return vo;
    }

    private void syncCurrentVersion(FormConfigEntity config) {
        Integer current = formConfigVersionRepository
                .findByFormConfigIdAndStatus(config.getFormConfigId(), FormConfigVersionStatus.PUBLISHED)
                .stream()
                .map(FormConfigVersionEntity::getVersion)
                .max(Integer::compareTo)
                .orElse(0);
        config.setCurrentVersion(current);
        formConfigRepository.save(config);
        formConfigVersionRepository.findByFormConfigIdOrderByVersionDesc(config.getFormConfigId())
                .forEach(version -> {
                    boolean isCurrent = current > 0
                            && version.getStatus() == FormConfigVersionStatus.PUBLISHED
                            && version.getVersion().equals(current);
                    if (!Boolean.valueOf(isCurrent).equals(version.getCurrent())) {
                        version.setCurrent(isCurrent);
                        formConfigVersionRepository.save(version);
                    }
                });
    }

    private Predicate buildPredicate(FormConfigQuery query) {
        BooleanBuilder builder = new BooleanBuilder();
        com.github.zeng.alt.workflow.entity.QFormConfigEntity q =
                com.github.zeng.alt.workflow.entity.QFormConfigEntity.formConfigEntity;
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

    private FormConfigEntity getRequiredEntity(Long id) {
        return formConfigRepository.findById(id)
                .getOrElseThrow(() -> new BaseException("配置表单不存在: " + id));
    }

    private FormConfigVersionEntity getRequiredVersion(Long versionId) {
        return formConfigVersionRepository.findById(versionId)
                .getOrElseThrow(() -> new BaseException("配置表单版本不存在: " + versionId));
    }
}
