package com.github.zeng.alt.excel.web;

import io.vavr.control.Try;

import java.util.Iterator;
import java.util.List;

/**
 * RxJava 类型的适配点，让 Web 集成在不引用任何 RxJava 类型的前提下支持
 * {@code Flowable} 形状的导入导出。
 * <p>
 * RxJava 是本模块的<b>可选</b>依赖：没引入时装配的是
 * {@link NoOpExcelReactiveSupport}，集合形状照常工作，只有 {@code Flowable} 形状
 * 会给出「请引入 rxjava」的明确报错；引入后自动换成
 * {@link RxJavaExcelReactiveSupport}。
 * <p>
 * 本接口与具体 Web 栈无关：上传文件在 Servlet 栈是 {@code MultipartFile}、在
 * WebFlux 栈是 {@code FilePart}，两者都由各自的集成包成
 * {@link ExcelStreamSource} 再递进来。
 * <p>
 * 方法都用 {@code Object} 承载响应式值——这是把可选依赖挡在核心代码之外的代价，
 * 也是它唯一的用途；业务代码不该直接用本接口，用
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
     * 把若干个懒打开的读取来源串成一条流。
     * <p>
     * 实现必须保证：订阅时才调用 {@link ExcelStreamSource#open()}，流终结
     * （完成、出错、取消）时调用 {@link ExcelStreamSource#close()}。
     *
     * @param sources 读取来源，按顺序拼接
     * @return 行流
     */
    Object streamOf(List<ExcelStreamSource> sources);

    /**
     * 把响应式值转成游标，供写出侧分批阻塞拉取。
     * <p>
     * 会阻塞当前线程，调用方负责把它放在允许阻塞的线程上（WebFlux 集成放在
     * {@code boundedElastic}）。
     *
     * @param reactiveValue 响应式值
     * @return 行游标
     */
    Try<Iterator<Object>> iterator(Object reactiveValue);
}
