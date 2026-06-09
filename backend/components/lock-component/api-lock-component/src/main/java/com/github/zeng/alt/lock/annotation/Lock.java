package com.github.zeng.alt.lock.annotation;

import com.github.zeng.alt.lock.executor.LockExecutor;
import com.github.zeng.alt.lock.model.LockFailureStrategy;
import com.github.zeng.alt.lock.model.LockKeyBuilder;
import java.lang.annotation.*;

/**
 * 分布式锁注解，支持 SpEL 表达式解析 key

 * @author zengJiaJun
 * @since 2026年06月09日
 * @version 1.0
 */
@Repeatable(Lock.List.class)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Lock {

    /**
     * 条件表达式，当结果为 {@code true} 或 {@code 'true'} 时才执行锁操作
     */
    String condition() default "";

    /**
     * 锁资源名称，为空则使用 包名+类名+方法名
     */
    String name() default "";

    /**
     * 锁 key 后缀（支持 SpEL 表达式），最终 key = prefix:name#keys
     */
    String[] keys() default {};

    /**
     * 锁过期时间（毫秒），默认 -1 使用全局配置
     */
    long expire() default -1;

    /**
     * 获取锁超时时间（毫秒），默认 -1 使用全局配置
     */
    long acquireTimeout() default -1;

    /**
     * 锁执行器
     */
    Class<? extends LockExecutor> executor() default LockExecutor.class;

    /**
     * 方法执行完成后是否自动释放锁
     */
    boolean autoRelease() default true;

    /**
     * 锁失败策略
     */
    Class<? extends LockFailureStrategy> failStrategy()
            default LockFailureStrategy.class;

    /**
     * key 构建器策略
     */
    Class<? extends LockKeyBuilder> keyBuilderStrategy()
            default LockKeyBuilder.class;

    /**
     * 可重复注解容器
     */
    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    @interface List {

        Lock[] value();

    }
}