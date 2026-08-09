package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新业务命令（部分更新，null 字段不修改）
 *
 * @author zengAlt
 */
@Data
@Schema(name = "更新业务请求")
public class BusinessUpdateCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "业务名称")
    private String name;

    @Schema(name = "父业务ID")
    private Long parentId;

    @Schema(name = "排序号")
    private Integer sortOrder;

    @Schema(name = "关联配置表单ID")
    private Long formConfigId;

    @Schema(name = "业务描述")
    private String description;

    @Schema(name = "备注")
    private String remark;
}
