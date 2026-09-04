package com.github.zeng.alt.excel.annotation;

import com.github.zeng.alt.excel.read.ExcelErrorPolicy;
import com.github.zeng.alt.excel.read.ExcelReadResult;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * 把上传的 Excel 文件直接解析成 controller 方法参数。
 * <p>
 * 支持三种参数形状，按数据量选：
 * <ul>
 *   <li>{@code List<T>} —— 小文件，全量装入内存；坏行按 {@link #onError()} 处理。
 *       这个形状承载不了失败明细，所以策略是 {@code FAIL_FAST} / {@code COLLECT_ALL}
 *       且确实有错时，会抛 {@code ExcelValidationException}（内含可直接给前端的报告）</li>
 *   <li>{@link ExcelReadResult}{@code <T>} —— 小文件，同时拿到成功行与失败明细（推荐）</li>
 *   <li>{@code Flowable<T>} —— 大文件，带背压逐行下发；上传内容会先落到临时文件，
 *       订阅时才解析，因此可以安全地在请求线程之外消费。<b>需要自行引入
 *       {@code io.reactivex.rxjava3:rxjava}</b>，它是本模块的可选依赖</li>
 * </ul>
 * 用法：
 * <pre>{@code
 * @PostMapping("/import")
 * public RestResponse<Void> importUsers(@ExcelImport("file") ExcelReadResult<UserImportCmd> result) {
 *     return userService.batchCreate(result).fold(RestResponse::fail, RestResponse::success);
 * }
 * }</pre>
 * 注意本注解不使用 {@code @AliasFor}：{@code @AliasFor} 只在经过
 * {@code MergedAnnotations} 读取时才生效，而参数解析器拿到的是原生注解实例，
 * 别名不会被处理。文件字段名统一用 {@link #value()}，留空则取参数名。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelImport {

    /**
     * multipart 表单里的文件字段名，留空则取方法参数名。
     *
     * @return 字段名
     */
    String value() default "";

    /**
     * 缺少文件时是否报错。
     * <p>
     * {@code false} 时缺文件返回空集合 / 空流 / 空结果，而不是抛异常。
     *
     * @return true 表示必填
     */
    boolean required() default true;

    /**
     * 是否读取列数不定的表，参数元素类型需实现
     * {@link com.github.zeng.alt.excel.dynamic.DynamicColumn}。
     *
     * @return true 表示按动态列读取
     */
    boolean dynamic() default false;

    /**
     * 同名字段上传了多个文件时，是否把所有文件的行合并成一个集合 / 一条流。
     * <p>
     * {@code false} 时只取第一个文件。
     *
     * @return true 表示合并
     */
    boolean merge() default false;

    /**
     * 表头占用的行数，{@code -1} 表示取配置项 {@code alt.excel.read.head-row-number}。
     *
     * @return 表头行数
     */
    int headRowNumber() default -1;

    /**
     * 是否对每行执行 Bean Validation。
     *
     * @return true 表示校验
     */
    boolean validate() default true;

    /**
     * Bean Validation 的校验分组。
     * <p>
     * 非空时隐式打开 {@link #validate()}。不指定则按 jakarta 的默认分组
     * （{@code Default.class}）校验。
     *
     * @return 校验分组
     */
    Class<?>[] validationGroups() default {};

    /**
     * 坏行（解析失败或校验不通过）策略，默认取配置项 {@code alt.excel.read.on-error}。
     * <p>
     * 三种语义见 {@link ExcelErrorPolicy}。参数形状是 {@code ExcelReadResult<T>} 时
     * 三种策略都只体现在结果里（不抛异常）；形状是 {@code List<T>} /
     * {@code Flux<T>} / {@code Flowable<T>} 时，整单驳回只能靠抛
     * {@code ExcelValidationException} 表达。
     *
     * @return 坏行策略
     */
    ExcelErrorPolicy onError() default ExcelErrorPolicy.SKIP_ROW;

    /**
     * 是否按国际化文本匹配表头，见
     * {@link com.github.zeng.alt.excel.read.ExcelReadSpec#i18nHead(boolean)}。
     *
     * @return true 表示开启
     */
    boolean i18nHead() default false;
}
