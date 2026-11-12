package com.github.zeng.alt.captcha.core;

import com.github.zeng.alt.captcha.config.CaptchaProperties;
import com.github.zeng.alt.captcha.model.CaptchaType;
import com.github.zeng.alt.captcha.producer.ArithmeticCaptchaProducer;
import com.github.zeng.alt.captcha.producer.RandomCodeProducer;
import com.github.zeng.alt.storage.CacheStringOperations;
import com.github.zeng.alt.storage.StorageTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证真实图片生成器与验证码缓存的一次消费约定。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CaptchaTemplateImplTest {
    private final StorageTemplate storage = mock(StorageTemplate.class);
    private final CacheStringOperations strings = mock(CacheStringOperations.class);
    private final Map<String, String> answers = new HashMap<>();
    private CaptchaTemplateImpl captcha;

    @BeforeEach
    void setUp() {
        final var renderer = new CaptchaRenderer(120, 60);
        captcha = new CaptchaTemplateImpl(storage, new CaptchaProperties(), List.of(
                new RandomCodeProducer(renderer, 4), new ArithmeticCaptchaProducer(renderer)));
        when(storage.opsForString()).thenReturn(strings);
        doAnswer(invocation -> {
            answers.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(strings).set(anyString(), anyString(), any(Duration.class));
        when(strings.get(anyString(), eq(String.class)))
                .thenAnswer(invocation -> answers.get(invocation.<String>getArgument(0)));
        when(storage.delete(anyString()))
                .thenAnswer(invocation -> answers.remove(invocation.<String>getArgument(0)) != null);
    }

    @ParameterizedTest
    @EnumSource(CaptchaType.class)
    void shouldGeneratePngAndAcceptCachedAnswerOnlyOnce(CaptchaType type) throws Exception {
        final var challenge = captcha.generate(type);
        final String cacheKey = "captcha:" + challenge.getKey();
        final String answer = answers.get(cacheKey);
        final var image = ImageIO.read(new ByteArrayInputStream(challenge.getBytes()));
        assertThat(image.getWidth()).isEqualTo(120);
        assertThat(image.getHeight()).isEqualTo(60);
        verify(strings).set(cacheKey, answer, Duration.ofSeconds(300));
        assertThat(captcha.verify(challenge.getKey(), answer.toLowerCase(Locale.ROOT))).isTrue();
        assertThat(captcha.verify(challenge.getKey(), answer)).isFalse();
    }

    @Test
    void shouldConsumeChallengeWhenAnswerIsWrong() {
        final var challenge = captcha.generate(CaptchaType.CODE);
        final String answer = answers.get("captcha:" + challenge.getKey());
        assertThat(captcha.verify(challenge.getKey(), "wrong")).isFalse();
        assertThat(captcha.verify(challenge.getKey(), answer)).isFalse();
    }

    @Test
    void shouldRejectExpiredOrMissingChallenge() {
        final var challenge = captcha.generate(CaptchaType.CODE);
        final String answer = answers.remove("captcha:" + challenge.getKey());
        assertThat(captcha.verify(challenge.getKey(), answer)).isFalse();
        assertThat(captcha.verify("missing", answer)).isFalse();
    }
}
