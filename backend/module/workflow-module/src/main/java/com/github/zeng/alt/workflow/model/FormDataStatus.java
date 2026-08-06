package com.github.zeng.alt.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 动态表单数据状态
 *
 * @author zengAlt
 */
@Schema(name = "动态表单数据状态")
public enum FormDataStatus {

    /** 草稿 */
    DRAFT,

    /** 已提交 */
    SUBMITTED,
}
