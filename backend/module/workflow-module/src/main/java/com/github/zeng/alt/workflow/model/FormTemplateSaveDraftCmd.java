package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 保存表单模板草稿命令
 *
 * @author zengAlt
 */
@Data
@Schema(name = "保存表单模板草稿请求")
public class FormTemplateSaveDraftCmd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "表单定义（FormKit FormDefinition JSON）")
    private String definition;
}