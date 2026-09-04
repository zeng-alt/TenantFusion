package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.read.ExcelErrorPolicy;

import lombok.Getter;
import lombok.Setter;
import org.apache.fesod.sheet.enums.CacheLocationEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Locale;

/**
 * Excel 组件配置属性。
 * <p>
 * 前缀 {@code alt.excel}。旧版本用的是 {@code fast.excel}，且直接把
 * fesod 的 {@code GlobalConfiguration} 当成 {@code @ConfigurationProperties}
 * 目标——那样没有配置元数据、也不在项目自己的根命名空间下，已改成本类。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "alt.excel")
public class ExcelProperties {

    /** 单元格文本是否去除首尾空白 */
    private boolean autoTrim = true;

    /** 是否按 1904 日期系统解释日期（Mac 版 Excel 的旧行为） */
    private boolean use1904windowing = false;

    /** 读取数字时是否保留科学计数法原样 */
    private boolean useScientificFormat = false;

    /** 字段元数据缓存位置：THREAD_LOCAL / MEMORY / NONE */
    private CacheLocationEnum fieldCacheLocation = CacheLocationEnum.THREAD_LOCAL;

    /** 默认 Locale，留空则用 JVM 默认 */
    private Locale locale;

    /**
     * 实体与单元格之间的绑定方式，默认按运行环境自动选。
     * <p>
     * native image 下必须是 {@code REFLECTIVE}（{@code AUTO} 会自动切过去）——
     * fesod 自己的实体绑定走 cglib 运行期生成字节码，native 不支持。
     */
    private ExcelBindingMode binding = ExcelBindingMode.AUTO;

    @NestedConfigurationProperty
    private final Read read = new Read();

    @NestedConfigurationProperty
    private final Write write = new Write();

    @NestedConfigurationProperty
    private final Web web = new Web();

    /**
     * 读取相关默认值。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Getter
    @Setter
    public static class Read {

        /** 表头占用的行数 */
        private int headRowNumber = 1;

        /** 是否对每行执行 Bean Validation */
        private boolean validate = true;

        /**
         * 坏行策略：SKIP_ROW 跳过坏行继续（部分成功）、FAIL_FAST 首个坏行即中断、
         * COLLECT_ALL 校验完整个文件再整单驳回。
         */
        private ExcelErrorPolicy onError = ExcelErrorPolicy.SKIP_ROW;

        /** 失败明细上限，超出后停止解析，避免坏文件把内存刷爆 */
        private int maxErrors = 1000;

        /** 是否按国际化文本匹配表头 */
        private boolean i18nHead = false;

        /** 是否忽略全空行 */
        private boolean ignoreEmptyRow = true;
    }

    /**
     * 写出相关默认值。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Getter
    @Setter
    public static class Write {

        /** 列宽是否按内容自适应 */
        private boolean autoWidth = true;

        /** 表头是否做国际化替换 */
        private boolean i18nHead = true;

        /** 是否全部在内存中生成（快但吃内存，大数据量务必保持 false） */
        private boolean inMemory = false;

        /** 从 Flowable 写出时的分批大小 */
        private int batchSize = 2000;
    }

    /**
     * Web 集成相关默认值。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Getter
    @Setter
    public static class Web {

        /** 是否启用 @ExcelImport / @ExcelExport 的 MVC 集成 */
        private boolean enabled = true;

        /**
         * 解析成 {@code Flowable} 时临时文件的存放目录，留空用系统临时目录。
         * <p>
         * 这条路径是懒订阅的，而 multipart 的原始存储在请求结束时就被容器回收了，
         * 所以上传内容必须先落盘；文件在流终结（完成、出错、取消）时删除。
         */
        private String tempDir;
    }
}
