package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.vavr.control.Try;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.function.Function;

/**
 * 响应式类型的适配点，让 Web 层在不引用任何 RxJava 类型的前提下支持
 * {@code Flowable} 形状的导入导出。
 * <p>
 * RxJava 是本模块的<b>可选</b>依赖：没引入时装配的是
 * {@link NoOpExcelReactiveSupport}，{@code @ExcelImport} / {@code @ExcelExport}
 * 的集合形状照常工作，只有响应式形状会给出「请引入 rxjava」的明确报错；引入后
 * 自动换成 {@code RxJavaExcelReactiveSupport}。
 * <p>
 * 所有方法都用 {@code Object} 承载响应式值——这是把可选依赖挡在核心代码之外的
 * 代价，也是它唯一的用途；业务代码不该直接用本接口，用
 * {@code com.github.zeng.alt.excel.rx.RxExcel}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public interface ExcelReactiveSupport {

    /**
     * 该类型是否由本适配器负责。
     *
     * @param type 方法参数或返回值的类型
     * @return true 表示是本适配器能产出/消费的响应式类型
     */
    boolean supports(Class<?> type);

    /**
     * 一条不产出任何行的流，用于「文件非必填且没上传」的情形。
     *
     * @return 空流
     */
    Object emptyStream();

    /**
     * 把若干上传文件懒解析成一条流。
     * <p>
     * 实现必须保证：订阅时才落盘与解析，流终结（完成、出错、取消）时删除临时文件。
     * 之所以要落盘——multipart 的原始存储在请求结束时就被 servlet 容器回收了，
     * 而流是懒执行的，订阅时再去读原始流必然失败。
     *
     * @param files       上传文件
     * @param tempDir     临时目录，空则用系统临时目录
     * @param specFactory 由临时文件建读取链
     * @return 行流
     */
    Object streamOf(List<MultipartFile> files, String tempDir, Function<File, ExcelReadSpec<?>> specFactory);

    /**
     * 从响应式返回值写出。
     *
     * @param spec          已配置好输出目标的写出链
     * @param reactiveValue 控制器返回的响应式值
     * @return 写出的行数
     */
    Try<Long> write(ExcelWriteSpec<Object> spec, Object reactiveValue);
}
