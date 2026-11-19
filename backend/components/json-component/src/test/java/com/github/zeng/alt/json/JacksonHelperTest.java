package com.github.zeng.alt.json;

import com.github.zeng.alt.api.exception.UtilException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.core.JacksonException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 JSON 工具迁移后的输入边界与异常包装。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class JacksonHelperTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JsonConfiguration.class, JacksonAutoConfiguration.class));

    @Test
    void readsStringsBytesAndArraysAndConvertsObjects() {
        runner.run(context -> {
            final JacksonHelper helper = context.getBean(JacksonHelper.class);
            final String json = "{\"name\":\"example\"}";
            assertThat(helper.parseObject(json, Map.class)).containsEntry("name", "example");
            assertThat(helper.parseObject(json.getBytes(StandardCharsets.UTF_8), Map.class))
                    .containsEntry("name", "example");
            assertThat(helper.parseArray("[\"first\",\"second\"]", String.class))
                    .containsExactly("first", "second");
            assertThat(helper.toMap(Map.of("name", "example"))).containsEntry("name", "example");
            assertThat(helper.toJsonString(Map.of("name", "example"))).isEqualTo(json);
        });
    }

    @Test
    void retainsEmptyInputBehavior() {
        runner.run(context -> {
            final JacksonHelper helper = context.getBean(JacksonHelper.class);
            assertThat(helper.toJsonString(List.of())).isNull();
            assertThat(helper.toMap(null)).isEmpty();
            assertThat(helper.parseObject(" ", Map.class)).isNull();
            assertThat(helper.parseObject(new byte[0], Map.class)).isNull();
            assertThat(helper.parseArray(" ", String.class)).isEmpty();
        });
    }

    @Test
    void wrapsJackson3ParsingFailures() {
        runner.run(context -> {
            final JacksonHelper helper = context.getBean(JacksonHelper.class);
            assertThatThrownBy(() -> helper.parseObject("{", Map.class))
                    .isInstanceOf(UtilException.class).hasCauseInstanceOf(JacksonException.class);
            assertThatThrownBy(() -> helper.parseObject(new byte[]{'{'}, Map.class))
                    .isInstanceOf(UtilException.class).hasCauseInstanceOf(JacksonException.class);
            assertThatThrownBy(() -> helper.parseArray("{", String.class))
                    .isInstanceOf(UtilException.class).hasCauseInstanceOf(JacksonException.class);
        });
    }
}
