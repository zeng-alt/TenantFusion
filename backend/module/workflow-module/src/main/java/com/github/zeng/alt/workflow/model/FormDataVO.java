package com.github.zeng.alt.workflow.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.workflow.entity.FormDataEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 动态表单数据视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "动态表单数据")
public class FormDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "数据ID")
    private Long formDataId;

    @Schema(name = "表单模板ID")
    private Long formTemplateId;

    @Schema(name = "表单模板名称")
    private String formTemplateName;

    @Schema(name = "表单模板版本快照")
    private Integer formVersion;

    @Schema(name = "关联流程实例ID")
    private String processInstanceId;

    @Schema(name = "表单字段值（JSON：字段名 → 值）")
    private JsonNode data;

    @Schema(name = "数据状态")
    private FormDataStatus status;

    @Schema(name = "提交时间")
    private LocalDateTime submittedDate;

    @Schema(name = "备注")
    private String remark;

    @Schema(name = "创建人")
    private String createdBy;

    @Schema(name = "创建时间")
    private LocalDateTime createdDate;

    @Schema(name = "更新时间")
    private LocalDateTime lastModifiedDate;

    /**
     * 从实体构造 VO
     *
     * @param entity        表单数据实体
     * @param objectMapper  JSON 序列化器
     * @return VO
     */
    public static FormDataVO from(FormDataEntity entity, ObjectMapper objectMapper) {
        return from(entity, objectMapper, null);
    }

    /**
     * 从实体构造 VO
     *
     * @param entity            表单数据实体
     * @param objectMapper      JSON 序列化器
     * @param formTemplateName 表单模板名称
     * @return VO
     */
    public static FormDataVO from(FormDataEntity entity, ObjectMapper objectMapper, String formTemplateName) {
        if (entity == null) {
            return null;
        }
        return FormDataVO.builder()
                .formDataId(entity.getFormDataId())
                .formTemplateId(entity.getFormTemplateId())
                .formTemplateName(formTemplateName)
                .formVersion(entity.getFormVersion())
                .processInstanceId(entity.getProcessInstanceId())
                .data(parseData(entity.getData(), objectMapper))
                .status(entity.getStatus())
                .submittedDate(entity.getSubmittedDate())
                .remark(entity.getRemark())
                .createdBy(entity.getCreatedBy().orElse(null))
                .createdDate(entity.getCreatedDate().orElse(null))
                .lastModifiedDate(entity.getLastModifiedDate().orElse(null))
                .build();
    }

    /**
     * 解析数据 JSON 字符串；解析失败时返回 null
     *
     * @param data         数据 JSON 字符串
     * @param objectMapper JSON 序列化器
     * @return 数据对象
     */
    private static JsonNode parseData(String data, ObjectMapper objectMapper) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}