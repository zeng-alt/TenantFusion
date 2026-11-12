package com.github.zeng.alt.captcha.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证验证码输出合法 PNG，并可在没有 java.desktop 的环境中生成。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class CaptchaRendererTest {
    @ParameterizedTest
    @CsvSource({"120,60", "160,80", "240,90"})
    void shouldProduceDecodablePngWithConfiguredDimensions(int width, int height) throws Exception {
        final byte[] png = new CaptchaRenderer(width, height).render("A2BC");
        final var image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(width);
        assertThat(image.getHeight()).isEqualTo(height);
        assertThat(new String(png, StandardCharsets.ISO_8859_1)).doesNotContain("A2BC");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDEFGHJKLMNPQRSTUVWXYZ23456789", "1+2=?", "9-3=?", "6×7=?", "8÷2=?"})
    void shouldDrawRandomCodeAlphabetAndArithmeticSymbols(String text) throws Exception {
        final var renderer = new CaptchaRenderer(text.length() * 30, 60);
        final var image = ImageIO.read(new ByteArrayInputStream(renderer.render(text)));
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == ' ') {
                continue;
            }
            final int left = index * 30;
            final long darkPixels = IntStream.range(0, image.getHeight())
                    .flatMap(y -> IntStream.range(left, left + 30).map(x -> image.getRGB(x, y)))
                    .filter(color -> ((color >>> 16) & 255) < 120
                            && ((color >>> 8) & 255) < 120 && (color & 255) < 120)
                    .count();
            assertThat(darkPixels).as("glyph %s", text.charAt(index)).isPositive();
        }
    }

    @Test
    void shouldVaryImageForTheSameAnswer() {
        final var renderer = new CaptchaRenderer(120, 60);
        assertThat(renderer.render("A2BC")).isNotEqualTo(renderer.render("A2BC"));
    }

    @Test
    void shouldRenderWithoutDesktopModule() throws Exception {
        final String classes = Path.of(
                CaptchaRenderer.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        ) + java.io.File.pathSeparator
                + Path.of(RenderProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        final var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "--limit-modules=java.base", "-cp", classes, RenderProbe.class.getName())
                .redirectErrorStream(true).start();
        assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.exitValue()).withFailMessage(output).isZero();
        assertThat(output).contains("PNG_OK");
    }

    /**
     * 在仅包含 java.base 的子进程中验证生产渲染器。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    public static class RenderProbe {
        public static void main(String[] args) {
            final byte[] png = new CaptchaRenderer(120, 60).render("A2BC");
            if (png.length < 8 || png[0] != (byte) 137 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
                throw new AssertionError("Invalid PNG output");
            }
            System.out.println("PNG_OK");
        }
    }
}
