package com.github.zeng.alt.workflow.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zeng.alt.workflow.entity.GlobalFormDataEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程全局表单数据视图对象
 *
 * @author zengAlt
 */
@Data
@Builder
@Schema(name = "流程全局表单数据")
public class GlobalFormDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "数据ID")
    private Long globalFormDataId;

    @Schema(name = "运行时流程实例ID")
    private String processInstanceId;

    @Schema(name = "流程模板编码")
    private String workflowCode;

    @Schema(name = "全局表单字段值（JSON：字段名 → 值）")
    private JsonNode data;

    @Schema(name = "发起流程时保存的全局表单定义快照（CAMUNDA 类型不含 FormKit 定义，实时解析最新版本）")
    private JsonNode definition;

    @Schema(name = "提交时间")
    private LocalDateTime submittedDate;

    @Schema(name = "创建人")
    private String createdBy;

    @Schema(name = "创建时间")
    private LocalDateTime createdDate;

    @Schema(name = "最后修改人")
    private String lastModifiedBy;

    @Schema(name = "最后修改时间")
    private LocalDateTime lastModifiedDate;

    /**
     * 从实体构造 VO
     *
     * @param entity       全局表单数据实体
     * @param objectMapper JSON 序列化器
     * @return VO
     */
    public static GlobalFormDataVO from(GlobalFormDataEntity entity, ObjectMapper objectMapper) {
        if (entity == null) {
            return null;
        }
        return GlobalFormDataVO.builder()
                .globalFormDataId(entity.getGlobalFormDataId())
                .processInstanceId(entity.getProcessInstanceId())
                .workflowCode(entity.getWorkflowCode())
                .data(parseData(entity.getData(), objectMapper))
                .definition(parseData(entity.getDefinition(), objectMapper))
                .submittedDate(entity.getSubmittedDate())
                .createdBy(entity.getCreatedBy().orElse(null))
                .createdDate(entity.getCreatedDate().orElse(null))
                .lastModifiedBy(entity.getLastModifiedBy().orElse(null))
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
