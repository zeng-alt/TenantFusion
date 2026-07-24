package com.github.zeng.alt.domain.validation;


import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native Image 运行时提示注册。
 * <p>
 * 为 {@code UniqueCheck} 注解、校验器及其 SPI 注册反射/资源 hints，
 * 确保在 AOT 编译后运行时能正常加载。
 *
 * @author zengJiaJun
 * @since 2026年07月23日
 */
public class ValidationRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(UniqueCheck.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(UniqueValidator.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(UniqueCheckServiceHolder.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(IUniqueCheckRepository.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection().registerType(EntityManagerUniqueCheckRepository.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}
