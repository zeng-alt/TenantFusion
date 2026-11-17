package com.github.zeng.alt.log;

import com.github.zeng.alt.log.core.aot.LogRuntimeHints;
import com.github.zeng.alt.log.core.operation.AnnotationLogOperationSource;
import com.github.zeng.alt.log.core.operation.LogOperation;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 接口注解、缓存与 Native 元数据的契约。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class LogOperationSourceTest {
    @Test
    void readsInterfaceAnnotationsAndCachesMatchesAndMisses() throws Exception {
        final var source = new AnnotationLogOperationSource();
        final var method = Action.class.getMethod("perform");
        final LogOperation operation = source.getLogOperation(method, Target.class);
        assertThat(operation.getTitle()).isEqualTo("interface");
        assertThat(source.getLogOperation(method, Target.class)).isSameAs(operation);
        final var missing = Object.class.getMethod("toString");
        assertThat(source.getLogOperation(missing, Target.class)).isNull();
        assertThat(source.getLogOperation(missing, Target.class)).isNull();
    }

    @Test
    void registersLogPayloadsForNativeSerialization() {
        final RuntimeHints hints = new RuntimeHints();
        new LogRuntimeHints().registerHints(hints, getClass().getClassLoader());
        assertThat(hints.reflection().getTypeHint(OperLogEvent.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(LoginInfoEvent.class)).isNotNull();
        assertThat(RuntimeHintsPredicates.resource().forResource("ua_parser/regexes.yaml").test(hints)).isTrue();
    }

    interface Action {
        @Log(title = "interface")
        void perform();
    }

    static class Target implements Action {
        @Override public void perform() { }
    }
}
