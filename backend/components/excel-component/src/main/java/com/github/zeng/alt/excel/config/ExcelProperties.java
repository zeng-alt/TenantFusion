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

    /**
     * 组件总开关。关掉之后连 {@code ExcelTemplate} 都不装配，整个模块等于不存在——
     * 只在「依赖被别的模块传递进来但本应用确实不用 Excel」时才需要。
     * 只想关注解集成的话用 {@code alt.excel.web.enabled}。
     */
    private boolean enabled = true;

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

        /**
         * 批量消费的每批条数，{@code consumeBatch} 攒够这么多行才回调一次。
         * <p>
         * 批量导入的瓶颈几乎总在下游写库：逐行 insert 一万次和五百行一批 insert
         * 二十次差一到两个数量级。默认 500 是「单条 SQL 参数量」与「失败重做成本」
         * 的折中；行很宽（几十列）时调小，很窄时可以到 2000。
         */
        private int batchSize = 500;

        /**
         * 单次读取的数据行上限，{@code -1} 表示不限。
         * <p>
         * 防的是「有人传了一份一百万行的文件」把内存和数据库一起打爆。超限按
         * {@link #onError} 处理：{@code SKIP_ROW} 下截断并记一条错误，
         * 另两种策略下整单驳回。
         */
        private int maxRows = -1;

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

        /** 从游标写出时的分批大小 */
        private int batchSize = 2000;

        /**
         * 单个 sheet 的数据行上限，超出自动开新 sheet（{@code Sheet1}、{@code Sheet2}…）。
         * <p>
         * xlsx 格式本身的硬上限是 1048576 行（含表头），撞上去 POI 会直接抛异常。
         * 默认留 100 万，给表头和多行表头留余量。{@code -1} 表示不分。
         */
        private int maxRowsPerSheet = 1_000_000;

        /**
         * 下载文件名里时间戳的格式，配合 {@code @ExcelExport(timestamp = true)}。
         * <p>
         * 之前硬编码成 {@code yyyyMMddHHmmss}；按天归档只想要 {@code yyyyMMdd} 的场景
         * 得能改。格式非法时退回默认值并记一条 warn，不让它把导出整个搞挂。
         */
        private String fileNameTimestampPattern = "yyyyMMddHHmmss";
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
