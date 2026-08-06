package com.github.zeng.alt.workflow.model;

/**
 * 表单模板版本状态（草稿 / 已发布 / 已下线）
 *
 * @author zengAlt
 */
public enum FormTemplateVersionStatus {

    /** 草稿：仅保存在本地表，尚未发布生效 */
    DRAFT,

    /** 已发布：已生效，可被表单数据使用 */
    PUBLISHED,

    /** 已下线：曾被发布，后手动下线停用 */
    OFFLINE
}
