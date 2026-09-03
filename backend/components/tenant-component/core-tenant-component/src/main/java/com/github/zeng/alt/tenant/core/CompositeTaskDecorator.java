package com.github.zeng.alt.tenant.core;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 串联多个 {@link TaskDecorator}。
 * <p>
 * Spring Boot 的 TaskExecutor 自动配置通过 {@code ObjectProvider#getIfUnique()} 取装饰器，
 * 容器里存在两个 {@code TaskDecorator} Bean 时它返回 null，结果<b>两个都不生效</b>。
 * 项目里已有 {@code UserAwareTaskDecorator}，再加租户装饰器就会踩到这个坑，
 * 因此本类标记为 {@code @Primary}，由它统一编排。
 *
 * @author zengJiaJun
 * @since 2026年09月03日
 * @version 1.0
 */
public class CompositeTaskDecorator implements TaskDecorator {

    private final ObjectProvider<TaskDecorator> provider;

    public CompositeTaskDecorator(ObjectProvider<TaskDecorator> provider) {
        this.provider = provider;
    }

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        Runnable result = runnable;
        // 倒序包装，使排序靠前的装饰器位于最外层
        List<TaskDecorator> delegates = delegates();
        for (int i = delegates.size() - 1; i >= 0; i--) {
            result = delegates.get(i).decorate(result);
        }
        return result;
    }

    /**
     * 延迟解析并剔除自身，避免构造期自引用导致的循环依赖。
     *
     * @return 排序后的委派列表
     */
    private List<TaskDecorator> delegates() {
        List<TaskDecorator> delegates = new ArrayList<>();
        provider.orderedStream()
                .filter(decorator -> decorator != this)
                .forEach(delegates::add);
        AnnotationAwareOrderComparator.sort(delegates);
        return delegates;
    }
}
