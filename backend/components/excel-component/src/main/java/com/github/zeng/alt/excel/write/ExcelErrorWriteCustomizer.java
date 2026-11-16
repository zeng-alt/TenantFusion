package com.github.zeng.alt.excel.write;

/**
 * 错误文件写出链的全局贡献者，按 Bean 的 Order 应用，可由单次链式调用继续覆盖。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@FunctionalInterface
public interface ExcelErrorWriteCustomizer {

    /** 配置当前新建的错误文件写出链。 */
    void customize(ExcelErrorWriteSpec spec);
}
