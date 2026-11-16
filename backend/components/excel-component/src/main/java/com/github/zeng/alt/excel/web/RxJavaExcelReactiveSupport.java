package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.rx.RxExcel;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@link ExcelReactiveSupport} 的 RxJava 实现。
 * <p>
 * 本类与 {@link RxExcel} 是整个模块唯一引用 RxJava 的两处，且只在 classpath 上有
 * {@code io.reactivex.rxjava3.core.Flowable} 时才会被装配，因此没引 rxjava 的应用
 * 连加载它都不会发生。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class RxJavaExcelReactiveSupport implements ExcelReactiveSupport {

    @Override
    public boolean supports(Class<?> type) {
        return type != null && Flowable.class.isAssignableFrom(type);
    }

    @Override
    public Object emptyStream() {
        return Flowable.empty();
    }

    @Override
    public Object streamOf(List<ExcelStreamSource> sources) {
        List<Flowable<?>> streams = new ArrayList<>(sources.size());
        for (ExcelStreamSource source : sources) {
            streams.add(lazyStream(source));
        }
        return Flowable.concat(streams);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<Iterator<Object>> iterator(Object reactiveValue) {
        return Try.of(() -> ((Flowable<Object>) reactiveValue).blockingIterable().iterator());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Try<Long> write(ExcelWriteSpec<Object> spec, Object reactiveValue) {
        return RxExcel.write(spec, (Flowable<Object>) reactiveValue);
    }

    /**
     * {@code Flowable.using} 保证订阅时才 open、流终结时一定 close。
     */
    private static Flowable<?> lazyStream(ExcelStreamSource source) {
        return Flowable.using(() -> source, s -> RxExcel.stream(s.open()), ExcelStreamSource::close)
                .subscribeOn(Schedulers.io(), false);
    }
}
