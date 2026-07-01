package com.github.zeng.alt.log.core.aot;

import com.github.zeng.alt.log.Log;
import com.github.zeng.alt.log.core.operation.LogOperation;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;


/**
 * @author zengJiaJun
 * @since 2026年07月01日
 * @version 1.0
 */
public class LogRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

        hints.reflection()
                .registerType(
                        Log.class,
                        MemberCategory.INTROSPECT_PUBLIC_METHODS,
                        MemberCategory.INVOKE_PUBLIC_METHODS
                );

        hints.reflection().registerType(
                LogOperation.class,
                MemberCategory.INVOKE_PUBLIC_METHODS);
    }

}