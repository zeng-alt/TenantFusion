package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务视图对象（含树形子节点）
 *
 * @author zengAlt
 */
@Data
@Schema(name = "业务")
public class BusinessVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "业务ID")
    private Long businessId;

    @Schema(name = "业务名称")
    private String name;

    @Schema(name = "业务编码")
    private String code;

    @Schema(name = "父业务ID")
    private Long parentId;

    @Schema(name = "排序号")
    private Integer sortOrder;

    @Schema(name = "关联配置表单ID")
    private Long formConfigId;

    @Schema(name = "关联配置表单名称")
    private String formConfigName;

    @Schema(name = "业务描述")
    private String description;

    @Schema(name = "备注")
    private String remark;

    @Schema(name = "创建人")
    private String createdBy;

    @Schema(name = "创建时间")
    private LocalDateTime createdDate;

    @Schema(name = "更新时间")
    private LocalDateTime lastModifiedDate;

    @Schema(name = "子业务列表")
    private List<BusinessVO> children;
}
