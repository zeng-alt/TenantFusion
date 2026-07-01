package com.github.zeng.alt.log;

/**
 * 业务操作类型
 *
 * @author zengJiaJun
 * @since 2026年07月01日
 * @version 1.0
 */
public enum BusinessType {
    /**
     * 其它
     */
    OTHER,

    /**
     * 新增
     */
    INSERT,

    /**
     * 修改
     */
    UPDATE,

    /**
     * 删除
     */
    DELETE,

    /**
     * 授权
     */
    GRANT,

    /**
     * 导出
     */
    EXPORT,

    /**
     * 导入
     */
    IMPORT,

    /**
     * 强退
     */
    FORCE,

    /**
     * 清空数据
     */
    CLEAN,
}