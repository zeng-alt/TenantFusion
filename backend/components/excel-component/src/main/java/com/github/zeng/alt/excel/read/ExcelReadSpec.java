package com.github.zeng.alt.excel.read;

import io.reactivex.rxjava3.core.Flowable;
import io.vavr.control.Try;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 读取链式配置面。
 * <p>
 * 形状固定为「一个数据源 + 若干可选项 + 一个终结步骤」，每个配置步骤返回 {@code this}，
 * 终结步骤按效果命名。三个终结步骤按数据量选：
 * <ul>
 *   <li>{@link #execute()} —— 小文件全量装入内存，成功行与失败明细一起拿到</li>
 *   <li>{@link #stream()} —— 大文件，带背压的响应式流，逐行下发</li>
 *   <li>{@link #consume(Consumer)} —— 大文件，只关心逐行副作用（入库、转发）</li>
 * </ul>
 * 典型用法：
 * <pre>{@code
 * ExcelReadResult<UserImportCmd> result = excelTemplate.read(UserImportCmd.class)
 *         .from(inputStream)
 *         .sheet(0)
 *         .skipInvalidRows(true)
 *         .execute();
 * return result.toEither().fold(this::renderErrors, userService::batchCreate);
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
     * 流由本组件负责关闭；{@link #stream()} 是懒执行的，订阅前不要在外部关掉它。
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
     * 坏行策略。
     * <p>
     * {@code true}：跳过该行、记入失败明细、继续读后面的行（批量导入的常态）。
     * <p>
     * {@code false}：遇到第一个坏行就停止解析剩余行。停止的表现方式取决于终结步骤：
     * {@link #execute()} 返回已读到的行加那一条失败明细（{@code toEither()} 因此是
     * {@code left}，「全有或全无」的语义在 {@code Either} 这一层成立）；
     * {@link #stream()} 以 {@code onError} 结束；{@link #consume(Consumer)} 返回
     * {@code Try.failure}。
     *
     * @param skipInvalidRows true 表示坏行跳过
     * @return this
     */
    ExcelReadSpec<T> skipInvalidRows(boolean skipInvalidRows);

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
     * 转成带背压的响应式流，逐行下发。
     * <p>
     * 懒执行：订阅时才真正开始解析，默认在 {@code Schedulers.io()} 上跑。
     * 单行失败时若 {@link #skipInvalidRows(boolean)} 为 {@code false}，
     * 流以 {@code onError} 结束。
     * <p>
     * 注意 ThreadLocal 上下文（SecurityContext、租户上下文）不会跨调度器传递，
     * 需要的值请在订阅前取出。
     *
     * @return 行流
     */
    Flowable<T> stream();

    /**
     * 逐行消费，只要副作用。
     *
     * @param consumer 每行的处理逻辑
     * @return 成功消费的行数；IO 或解析层面的失败包在 {@code Try} 里
     */
    Try<Long> consume(Consumer<T> consumer);
}
