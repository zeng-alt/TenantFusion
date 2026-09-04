package com.github.zeng.alt.excel.rx;

import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vavr.control.Try;

import java.util.List;
import java.util.function.Supplier;

/**
 * Excel 的 RxJava 适配入口。
 * <p>
 * <b>本类只在 classpath 上有 RxJava 时可用</b>——RxJava 是本模块的可选依赖，
 * 核心 SPI（{@link ExcelReadSpec} / {@link ExcelWriteSpec}）的签名里没有任何
 * 响应式类型，否则没引 rxjava 的应用一旦反射枚举实现类的方法就会抛
 * {@code NoClassDefFoundError}。想用 {@code Flowable} 就自己声明依赖：
 * <pre>{@code
 * implementation("io.reactivex.rxjava3:rxjava")
 * }</pre>
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public final class RxExcel {

    private RxExcel() {
    }

    /**
     * 把读取链转成带背压的响应式流，逐行下发。
     * <p>
     * 懒执行：订阅时才真正开始解析，默认在 {@code Schedulers.io()} 上跑。下游取消时
     * 通过 {@link ExcelReadSpec#consumeWhile} 把信号传下去，立刻停止解析剩余行，
     * 不会把整份文件读完。
     * <p>
     * 背压策略是 {@code BUFFER}：数据源是 POI 的 SAX 解析，没法按 {@code requested()}
     * 暂停在任意行，速度差由缓冲承担。
     * <p>
     * 注意 ThreadLocal 上下文（{@code SecurityContext}、租户上下文）不会跨调度器
     * 传递，需要的值请在订阅前取出。
     *
     * @param spec 已配置好数据源的读取链，只能订阅一次（输入流读完即废）
     * @param <T>  行类型
     * @return 行流
     */
    public static <T> Flowable<T> stream(ExcelReadSpec<T> spec) {
        return stream(() -> spec);
    }

    /**
     * 同 {@link #stream(ExcelReadSpec)}，但读取链在每次订阅时才创建，因此可重复订阅。
     *
     * @param specFactory 读取链工厂
     * @param <T>         行类型
     * @return 行流
     */
    public static <T> Flowable<T> stream(Supplier<ExcelReadSpec<T>> specFactory) {
        return Flowable.<T>create(emitter -> specFactory.get()
                        .consumeWhile(row -> {
                            emitter.onNext(row);
                            return !emitter.isCancelled();
                        })
                        .onFailure(cause -> failQuietly(emitter, cause))
                        .onSuccess(count -> completeQuietly(emitter)),
                BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 从响应式流写出，适合大数据量导出。
     * <p>
     * 按 {@code alt.excel.write.batch-size} 分批阻塞拉取——写文件本身是同步动作，
     * 这里的阻塞是链路最外层的终结步骤，不会藏在中间操作符里。
     *
     * @param spec 已配置好输出目标的写出链
     * @param rows 数据流
     * @param <T>  行类型
     * @return 写出的行数；IO 层面的失败包在 {@code Try} 里
     */
    public static <T> Try<Long> write(ExcelWriteSpec<T> spec, Flowable<T> rows) {
        return rows == null ? spec.write(List.of()) : spec.write(rows.blockingIterable().iterator());
    }

    private static <T> void failQuietly(FlowableEmitter<T> emitter, Throwable cause) {
        if (!emitter.isCancelled()) {
            emitter.onError(new ExcelReadException("Excel 解析失败", cause));
        }
    }

    private static <T> void completeQuietly(FlowableEmitter<T> emitter) {
        if (!emitter.isCancelled()) {
            emitter.onComplete();
        }
    }
}
