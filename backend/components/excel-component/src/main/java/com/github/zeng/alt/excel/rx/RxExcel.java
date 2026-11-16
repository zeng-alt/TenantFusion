package com.github.zeng.alt.excel.rx;

import com.github.zeng.alt.excel.exception.ExcelReadException;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.context.ExcelContextHandler;
import com.github.zeng.alt.excel.context.ExcelContextTemplate;
import com.github.zeng.alt.excel.support.ExcelDemandHelper;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vavr.control.Try;

import java.util.List;
import java.util.Iterator;
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
     * SAX 回调在 IO 线程等待下游需求，最多持有一条尚未请求的行；取消会唤醒等待。
     * <p>
     * 在 IO 线程恢复读取链创建时的上下文，解析结束后还原原状态。
     * 业务自行增加 observeOn 时，在其后通过 withContext 恢复消费者所需上下文。
     *
     * @param spec 已配置好数据源的读取链，只能订阅一次（输入流读完即废）
     * @param <T>  行类型
     * @return 行流
     */
    public static <T> Flowable<T> stream(ExcelReadSpec<T> spec) {
        return stream(() -> spec, spec.captureContext());
    }

    /**
     * 同 {@link #stream(ExcelReadSpec)}，但读取链在每次订阅时才创建，因此可重复订阅。
     *
     * @param specFactory 读取链工厂
     * @param <T>         行类型
     * @return 行流
     */
    public static <T> Flowable<T> stream(Supplier<ExcelReadSpec<T>> specFactory) {
        return stream(specFactory, ExcelContextTemplate.captureDefaults());
    }

    /** 使用显式快照，支持 Spring 贡献者或业务自行保存的任务上下文。 */
    public static <T> Flowable<T> stream(
            Supplier<ExcelReadSpec<T>> specFactory,
            ExcelContextHandler.Snapshot snapshot
    ) {
        return Flowable.defer(() -> {
            final ExcelDemandHelper demand = new ExcelDemandHelper();
            return Flowable.<T>create(emitter -> {
                try (ExcelContextHandler.Scope scope = snapshot.restore()) {
                    specFactory.get().consumeWhile(row -> {
                                if (!demand.awaitDemand() || emitter.isCancelled()) {
                                    return false;
                                }
                                emitter.onNext(row);
                                return !emitter.isCancelled();
                            })
                            .onFailure(cause -> failQuietly(emitter, cause))
                            .onSuccess(count -> completeQuietly(emitter));
                }
            }, BackpressureStrategy.MISSING)
                    .doOnRequest(demand::request)
                    .doOnCancel(demand::cancel)
                    // 请求必须直接唤醒解析线程，不能排在正在等待需求的同一 worker 后面。
                    .subscribeOn(Schedulers.io(), false);
        });
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
        if (rows == null) {
            return spec.write(List.of());
        }
        final Iterator<T> iterator = rows.blockingIterable(64).iterator();
        try {
            return spec.write(iterator);
        } finally {
            ((Disposable) iterator).dispose();
        }
    }

    /** 在业务新增的 observeOn 之后恢复下游回调上下文，不修改全局调度器。 */
    public static <T> Flowable<T> withContext(Flowable<T> rows, ExcelContextHandler.Snapshot snapshot) {
        return rows.lift(subscriber -> new ContextSubscriber<>(subscriber, snapshot));
    }

    /**
     * 信号作用域适配器。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    private record ContextSubscriber<T>(
            Subscriber<? super T> subscriber,
            ExcelContextHandler.Snapshot snapshot
    ) implements FlowableSubscriber<T> {

        @Override
        public void onSubscribe(Subscription subscription) {
            snapshot.decorate(() -> subscriber.onSubscribe(subscription)).run();
        }

        @Override
        public void onNext(T row) {
            snapshot.decorate(() -> subscriber.onNext(row)).run();
        }

        @Override
        public void onError(Throwable failure) {
            snapshot.decorate(() -> subscriber.onError(failure)).run();
        }

        @Override
        public void onComplete() {
            snapshot.decorate(subscriber::onComplete).run();
        }
    }

    private static <T> void failQuietly(FlowableEmitter<T> emitter, Throwable cause) {
        if (!emitter.isCancelled()) {
            emitter.tryOnError(new ExcelReadException("Excel 解析失败", cause));
        }
    }

    private static <T> void completeQuietly(FlowableEmitter<T> emitter) {
        if (!emitter.isCancelled()) {
            emitter.onComplete();
        }
    }
}
