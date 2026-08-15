package com.github.zeng.alt.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.workflow.entity.GlobalFormDataEntity;
import com.github.zeng.alt.workflow.model.GlobalFormDataQuery;
import com.github.zeng.alt.workflow.model.GlobalFormDataSubmitCmd;
import com.github.zeng.alt.workflow.model.GlobalFormDataVO;
import com.github.zeng.alt.workflow.model.GlobalFormDefinitionVO;
import com.github.zeng.alt.workflow.repository.GlobalFormDataRepository;
import com.github.zeng.alt.workflow.service.GlobalFormDataService;
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

/**
 * 流程全局表单数据服务实现
 *
 * @author zengAlt
 */
@CommonsLog
@Service
@RequiredArgsConstructor
public class GlobalFormDataServiceImpl implements GlobalFormDataService {

    private final GlobalFormDataRepository globalFormDataRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageRestResponse<GlobalFormDataVO> page(GlobalFormDataQuery query) {
        Predicate predicate = buildPredicate(query);
        Sort sort = Sort.by(Sort.Direction.fromOptionalString(query.getOrder()).orElse(Sort.Direction.DESC),
                query.getSort());
        Page<GlobalFormDataEntity> pageResult = globalFormDataRepository.findAll(predicate,
                PageRequest.of(query.getPageNo() - 1, query.getPageSize(), sort));
        List<GlobalFormDataVO> vos = pageResult.getContent().stream()
                .map(entity -> GlobalFormDataVO.from(entity, objectMapper)).toList();
        return PageRestResponse.of(vos, pageResult.getTotalElements(), query.getPageSize(), query.getPageNo());
    }

    private Predicate buildPredicate(GlobalFormDataQuery query) {
        BooleanBuilder builder = new BooleanBuilder();
        com.github.zeng.alt.workflow.entity.QGlobalFormDataEntity q =
                com.github.zeng.alt.workflow.entity.QGlobalFormDataEntity.globalFormDataEntity;
        if (StringUtils.hasText(query.getWorkflowCode())) {
            builder.and(q.workflowCode.containsIgnoreCase(query.getWorkflowCode()));
        }
        if (StringUtils.hasText(query.getProcessInstanceId())) {
            builder.and(q.processInstanceId.containsIgnoreCase(query.getProcessInstanceId()));
        }
        return builder;
    }

    @Override
    @Transactional
    public GlobalFormDataVO initialize(String processInstanceId, String workflowCode, GlobalFormDefinitionVO definition) {
        GlobalFormDataEntity entity = globalFormDataRepository
                .findFirstByProcessInstanceIdOrderByLastModifiedDateDesc(processInstanceId)
                .orElseGet(GlobalFormDataEntity::new);
        entity.setProcessInstanceId(processInstanceId);
        entity.setWorkflowCode(workflowCode);
        entity.setData("{}");
        entity.setDefinition(serializeDefinition(definition));
        entity.setSubmittedDate(LocalDateTime.now());
        GlobalFormDataEntity saved = globalFormDataRepository.save(entity);
        log.info("初始化全局表单数据: processInstanceId=" + processInstanceId + ", workflowCode=" + workflowCode);
        return GlobalFormDataVO.from(saved, objectMapper);
    }

    @Override
    @Transactional
    public GlobalFormDataVO submit(GlobalFormDataSubmitCmd cmd) {
        GlobalFormDataEntity entity = globalFormDataRepository
                .findFirstByProcessInstanceIdOrderByLastModifiedDateDesc(cmd.getProcessInstanceId())
                .orElseGet(GlobalFormDataEntity::new);
        entity.setProcessInstanceId(cmd.getProcessInstanceId());
        entity.setWorkflowCode(cmd.getWorkflowCode());
        entity.setData(serializeData(cmd.getData()));
        entity.setSubmittedDate(LocalDateTime.now());
        GlobalFormDataEntity saved = globalFormDataRepository.save(entity);
        log.info("提交全局表单数据: processInstanceId=" + saved.getProcessInstanceId()
                + ", id=" + saved.getGlobalFormDataId());
        return GlobalFormDataVO.from(saved, objectMapper);
    }

    @Override
    @Transactional(readOnly = true)
    public GlobalFormDataVO getByProcessInstanceId(String processInstanceId) {
        return globalFormDataRepository
                .findFirstByProcessInstanceIdOrderByLastModifiedDateDesc(processInstanceId)
                .map(entity -> GlobalFormDataVO.from(entity, objectMapper))
                .orElse(null);
    }

    /**
     * 将全局表单定义快照序列化为字符串存储
     *
     * @param definition 全局表单定义快照
     * @return 规范化后的 JSON 字符串
     */
    private String serializeDefinition(GlobalFormDefinitionVO definition) {
        if (definition == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (JsonProcessingException e) {
            log.warn("序列化全局表单定义失败: " + definition.getType(), e);
            return null;
        }
    }

    /**
     * 将字段值 JSON 序列化为字符串存储
     *
     * @param data 字段值（JSON 字符串）
     * @return 规范化后的 JSON 字符串
     */
    private String serializeData(String data) {
        if (data == null || data.isBlank()) {
            throw new BaseException("表单数据不能为空");
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(data));
        } catch (JsonProcessingException e) {
            throw new BaseException("表单数据不是合法的 JSON: " + e.getOriginalMessage());
        }
    }
}
