package com.github.zeng.alt.workflow.service.impl;

import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.workflow.entity.BusinessEntity;
import com.github.zeng.alt.workflow.mapper.BusinessMapper;
import com.github.zeng.alt.workflow.model.BusinessCreateCmd;
import com.github.zeng.alt.workflow.model.BusinessUpdateCmd;
import com.github.zeng.alt.workflow.model.BusinessVO;
import com.github.zeng.alt.workflow.model.FormConfigCreateCmd;
import com.github.zeng.alt.workflow.model.FormConfigVO;
import com.github.zeng.alt.workflow.repository.BusinessRepository;
import com.github.zeng.alt.workflow.repository.FormConfigRepository;
import com.github.zeng.alt.workflow.service.BusinessService;
import com.github.zeng.alt.workflow.service.FormConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 业务服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final FormConfigRepository formConfigRepository;
    private final FormConfigService formConfigService;
    private final BusinessMapper businessMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BusinessVO> list() {
        return buildList(toVOList(businessRepository.findAll()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessVO> tree() {
        return buildTree(toVOList(businessRepository.findAll()));
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessVO getDetail(Long id) {
        return toVO(getRequiredEntity(id));
    }

    @Override
    @Transactional
    public BusinessVO create(BusinessCreateCmd cmd) {
        if (businessRepository.existsByCode(cmd.getCode())) {
            throw new BaseException("业务编码已存在: " + cmd.getCode());
        }
        validateParent(cmd.getParentId());
        BusinessEntity entity = businessMapper.toEntity(cmd);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        BusinessEntity saved = businessRepository.save(entity);
        log.info("创建业务: " + saved.getCode() + ", id=" + saved.getBusinessId());
        return toVO(saved);
    }

    @Override
    @Transactional
    public BusinessVO update(Long id, BusinessUpdateCmd cmd) {
        BusinessEntity entity = getRequiredEntity(id);
        if (cmd.getParentId() != null && cmd.getParentId().equals(id)) {
            throw new BaseException("父业务不能是自己");
        }
        validateParent(cmd.getParentId(), id);
        // parentId / formConfigId 允许置空（移动为根节点 / 解除表单关联），故显式赋值
        entity.setParentId(cmd.getParentId());
        entity.setFormConfigId(cmd.getFormConfigId());
        businessMapper.merge(cmd, entity);
        BusinessEntity saved = businessRepository.save(entity);
        log.info("更新业务: " + saved.getCode() + ", id=" + id);
        return toVO(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getRequiredEntity(id);
        boolean hasChildren = businessRepository.findAll().stream()
                .anyMatch(b -> id.equals(b.getParentId()));
        if (hasChildren) {
            throw new BaseException("存在子业务，无法删除");
        }
        businessRepository.deleteById(id);
        log.info("删除业务: id=" + id);
    }

    @Override
    @Transactional
    public BusinessVO createAndBindFormConfig(Long id, FormConfigCreateCmd cmd) {
        BusinessEntity entity = getRequiredEntity(id);
        // 复用配置表单创建逻辑，自动生成 v1 草稿版本
        FormConfigVO config = formConfigService.create(cmd);
        entity.setFormConfigId(config.getFormConfigId());
        BusinessEntity saved = businessRepository.save(entity);
        log.info("创建并关联配置表单: businessId=" + id + ", formConfigId=" + config.getFormConfigId()
                + ", code=" + config.getCode());
        return toVO(saved);
    }

    /**
     * 校验父业务存在（创建场景）
     */
    private void validateParent(Long parentId) {
        validateParent(parentId, null);
    }

    /**
     * 校验父业务存在且不是自身（更新场景 excludeId 排除自身）
     */
    private void validateParent(Long parentId, Long excludeId) {
        if (parentId == null) {
            return;
        }
        if (excludeId != null && parentId.equals(excludeId)) {
            return;
        }
        if (businessRepository.findById(parentId).isEmpty()) {
            throw new BaseException("父业务不存在: " + parentId);
        }
    }

    private List<BusinessVO> toVOList(List<BusinessEntity> entities) {
        Map<Long, String> configNameMap = loadConfigNameMap(entities);
        return entities.stream()
                .sorted(Comparator.comparing(BusinessEntity::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(entity -> {
                    BusinessVO vo = businessMapper.toVO(entity);
                    Long configId = entity.getFormConfigId();
                    vo.setFormConfigName(configId == null ? null : configNameMap.get(configId));
                    return vo;
                })
                .toList();
    }

    private BusinessVO toVO(BusinessEntity entity) {
        if (entity == null) {
            return null;
        }
        BusinessVO vo = businessMapper.toVO(entity);
        if (entity.getFormConfigId() != null) {
            vo.setFormConfigName(formConfigRepository.findById(entity.getFormConfigId())
                    .map(config -> config.getName())
                    .getOrElse((String) null));
        }
        return vo;
    }

    private Map<Long, String> loadConfigNameMap(List<BusinessEntity> entities) {
        List<Long> configIds = entities.stream()
                .map(BusinessEntity::getFormConfigId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (configIds.isEmpty()) {
            return new HashMap<>();
        }
        return formConfigRepository.findByIdIn(configIds).stream()
                .collect(Collectors.toMap(
                        config -> config.getId(),
                        config -> config.getName(),
                        (a, b) -> a));
    }

    /**
     * 扁平列表 → 树形结构（顶层按 sortOrder 升序）
     */
    private List<BusinessVO> buildTree(List<BusinessVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, BusinessVO> map = vos.stream()
                .filter(vo -> vo.getBusinessId() != null)
                .collect(Collectors.toMap(BusinessVO::getBusinessId, Function.identity()));
        List<BusinessVO> roots = new ArrayList<>();
        for (BusinessVO vo : vos) {
            Long parentId = vo.getParentId();
            if (parentId == null || !map.containsKey(parentId)) {
                roots.add(vo);
                continue;
            }
            BusinessVO parent = map.get(parentId);
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(vo);
        }
        roots.forEach(BusinessServiceImpl::sortChildren);
        return roots;
    }

    private static void sortChildren(BusinessVO vo) {
        if (vo.getChildren() != null) {
            vo.getChildren().sort(Comparator.comparing(BusinessVO::getSortOrder,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            vo.getChildren().forEach(BusinessServiceImpl::sortChildren);
        }
    }

    /**
     * 扁平列表按 sortOrder 升序（树接口的兄弟节点已在 buildTree 内排序）
     */
    private List<BusinessVO> buildList(List<BusinessVO> vos) {
        return vos;
    }

    private BusinessEntity getRequiredEntity(Long id) {
        return businessRepository.findById(id)
                .getOrElseThrow(() -> new BaseException("业务不存在: " + id));
    }
}
