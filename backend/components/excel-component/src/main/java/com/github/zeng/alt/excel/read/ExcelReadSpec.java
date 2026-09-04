package com.github.zeng.alt.excel.read;

import com.github.zeng.alt.excel.config.ExcelBindingMode;
import io.vavr.control.Try;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 读取链式配置面。
 * <p>
 * 形状固定为「一个数据源 + 若干可选项 + 一个终结步骤」，每个配置步骤返回 {@code this}，
 * 终结步骤按效果命名，按数据量选：
 * <ul>
 *   <li>{@link #execute()} —— 小文件全量装入内存，成功行与失败明细一起拿到</li>
 *   <li>{@link #consume(Consumer)} —— 大文件，只关心逐行副作用（入库、转发）</li>
 *   <li>{@link #consumeWhile(Predicate)} —— 同上，但能提前收工</li>
 * </ul>
 * 需要 {@code Flowable} 的话用 {@code RxExcel.stream(spec)}：RxJava 是本模块的
 * <b>可选</b>依赖，因此响应式类型不出现在本接口的签名里——否则没引 rxjava 的应用
 * 一旦反射枚举实现类的方法就会抛 {@code NoClassDefFoundError}。
 * 典型用法：
 * <pre>{@code
 * ExcelReadResult<UserImportCmd> result = excelTemplate.read(UserImportCmd.class)
 *         .from(inputStream)
 *         .sheet(0)
 *         .onError(ExcelErrorPolicy.COLLECT_ALL)
 *         .execute();
 * return result.toEither("users.xlsx").fold(this::renderReport, userService::batchCreate);
 * }</pre>
 *
 * @param <T> 行类型
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface ExcelReadSpec<T> {

    // ==================== 数据源 ====================

    /**
     * 从输入流读取。
     * <p>
     * 流由本组件负责关闭。
     *
     * @param inputStream 输入流
     * @return this
     */
    ExcelReadSpec<T> from(InputStream inputStream);

    /**
     * 从本地文件读取。
     *
     * @param file 文件
     * @return this
     */
    ExcelReadSpec<T> from(File file);

    /**
     * 从本地路径读取。
     *
     * @param path 路径
     * @return this
     */
    ExcelReadSpec<T> from(Path path);

    // ==================== 可选项 ====================

    /**
     * 按下标指定工作表，默认第 0 张。
     *
     * @param sheetNo 0 基工作表下标
     * @return this
     */
    ExcelReadSpec<T> sheet(int sheetNo);

    /**
     * 按名称指定工作表。
     *
     * @param sheetName 工作表名
     * @return this
     */
    ExcelReadSpec<T> sheet(String sheetName);

    /**
     * 表头占用的行数，默认取配置项 {@code alt.excel.read.head-row-number}。
     *
     * @param headRowNumber 表头行数
     * @return this
     */
    ExcelReadSpec<T> headRowNumber(int headRowNumber);

    /**
     * 加密文件的打开密码。
     *
     * @param password 密码
     * @return this
     */
    ExcelReadSpec<T> password(String password);

    /**
     * 是否对每行执行 Bean Validation，默认取配置项 {@code alt.excel.read.validate}。
     *
     * @param validate true 开启校验
     * @return this
     */
    ExcelReadSpec<T> validate(boolean validate);

    /**
     * 指定 Bean Validation 的校验分组。
     * <p>
     * 传入非空分组会隐式打开校验开关——指定了分组却不校验没有意义。不调用本方法
     * 时按 jakarta 的默认分组（{@code Default.class}）校验。
     * <p>
     * 典型用法是同一个实体在「新增导入」和「更新导入」下有不同的必填项：
     * <pre>{@code
     * excelTemplate.read(UserImportCmd.class)
     *         .from(file)
     *         .validationGroups(OnCreate.class)
     *         .execute();
     * }</pre>
     *
     * @param groups 校验分组；空或 {@code null} 表示默认分组
     * @return 当前链
     */
    ExcelReadSpec<T> validationGroups(Class<?>... groups);

    /**
     * 坏行（解析失败或校验不通过）策略，默认取配置项 {@code alt.excel.read.on-error}。
     * <p>
     * 三种语义见 {@link ExcelErrorPolicy}：
     * <ul>
     *   <li>{@link ExcelErrorPolicy#SKIP_ROW} —— 部分成功，好行照常返回</li>
     *   <li>{@link ExcelErrorPolicy#FAIL_FAST} —— 马上中断，不再读后面的行</li>
     *   <li>{@link ExcelErrorPolicy#COLLECT_ALL} —— 读完整个文件收齐所有错误再整单驳回</li>
     * </ul>
     * 三种策略下 {@link #execute()} 都<b>不抛异常</b>，结局在
     * {@link ExcelReadResult#isAborted()} 里；{@link #consume(Consumer)} 与
     * {@link #consumeWhile(Predicate)} 在整单驳回时返回 {@code Try.failure}，
     * 因为它们没有地方承载失败明细。
     *
     * @param policy 坏行策略，{@code null} 忽略
     * @return this
     */
    ExcelReadSpec<T> onError(ExcelErrorPolicy policy);

    /**
     * 失败明细的条数上限，默认取配置项 {@code alt.excel.read.max-errors}。
     * <p>
     * 达到上限就停止解析，并在 {@link ExcelReadSummary#truncated()} 上打标记，
     * 让前端能提示「还有更多问题未列出」。存在的意义是防一份全是坏行的文件把
     * 内存刷爆——{@code COLLECT_ALL} 策略下尤其需要。
     *
     * @param maxErrors 上限，必须为正
     * @return this
     */
    ExcelReadSpec<T> maxErrors(int maxErrors);

    /**
     * 表头按国际化文本匹配字段。
     * <p>
     * 开启后 {@code @ExcelProperty("{user.name}")} 能对上表头写着「姓名」的文件——
     * 先把实体上的 i18n key 解析成当前 Locale 的文本，再按文本定位列，最后用 Spring
     * {@code ConversionService} 绑定到字段。代价是不走 fesod 的自定义 {@code Converter}，
     * 所以只在确有多语言导入需求时开启；默认关闭，走 fesod 原生的字面量匹配。
     * <p>
     * <b>要与导出成对开启。</b>{@code ExcelWriteSpec#i18nHead(boolean)} 打开时落盘的
     * 表头是「姓名」，此时读取端若不开本项，fesod 会拿 {@code {user.name}} 去字面量
     * 匹配「姓名」，一列都对不上、读出来全是空对象。默认值由
     * {@code alt.excel.write.i18n-head}（默认开）与 {@code alt.excel.read.i18n-head}
     * （默认关）分别给出，这两个默认值不成对——导出模板给用户填再导回来的场景，
     * 请把读取端也打开。
     *
     * @param i18nHead true 开启国际化表头匹配
     * @return this
     */
    ExcelReadSpec<T> i18nHead(boolean i18nHead);

    /**
     * 实体绑定方式，默认取配置项 {@code alt.excel.binding}（{@code AUTO}）。
     * <p>
     * {@code AUTO} 在 native image 里自动切成 {@link ExcelBindingMode#REFLECTIVE}——
     * fesod 自己的实体绑定走 cglib 运行期生成字节码，native 不支持。一般不需要在
     * 调用处指定，只在某个实体依赖 fesod 自定义 {@code Converter}、必须强制
     * {@link ExcelBindingMode#ENGINE} 时才用。
     *
     * @param binding 绑定方式，{@code null} 视为 {@code AUTO}
     * @return this
     */
    ExcelReadSpec<T> binding(ExcelBindingMode binding);

    // ==================== 终结步骤 ====================

    /**
     * 全量读入内存。
     * <p>
     * 适用于小文件（万级以内）。永不返回 {@code null}，也不因单行失败抛异常——
     * 坏行一律进失败明细。只有整份文件级别的失败（文件损坏、密码错误、未指定数据源）
     * 才抛 {@link com.github.zeng.alt.excel.exception.ExcelReadException}。
     *
     * @return 成功行 + 失败明细
     */
    ExcelReadResult<T> execute();

    /**
     * 逐行消费，只要副作用。内存占用与文件大小无关。
     *
     * @param consumer 每行的处理逻辑
     * @return 成功消费的行数；IO 或解析层面的失败包在 {@code Try} 里
     */
    Try<Long> consume(Consumer<T> consumer);

    /**
     * 逐行消费，可提前收工。
     * <p>
     * {@code consumer} 返回 {@code false} 时停止解析剩余行——本组件的响应式适配
     * （{@code RxExcel.stream}）就是靠它把下游的取消信号传下来，不至于把整份文件读完。
     *
     * @param consumer 每行的处理逻辑，返回 {@code false} 表示不再需要后续行
     * @return 实际消费的行数；IO 或解析层面的失败包在 {@code Try} 里
     */
    Try<Long> consumeWhile(Predicate<T> consumer);
}
