package com.github.zeng.alt.captcha.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

/**
 * 使用内置点阵字形生成 PNG，避免 Native 环境对桌面图形库和系统字体的依赖。
 */
public final class CaptchaRenderer {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<Character, String> GLYPHS = Map.ofEntries(
            Map.entry('A', "01110/10001/10001/11111/10001/10001/10001"),
            Map.entry('B', "11110/10001/10001/11110/10001/10001/11110"),
            Map.entry('C', "01111/10000/10000/10000/10000/10000/01111"),
            Map.entry('D', "11110/10001/10001/10001/10001/10001/11110"),
            Map.entry('E', "11111/10000/10000/11110/10000/10000/11111"),
            Map.entry('F', "11111/10000/10000/11110/10000/10000/10000"),
            Map.entry('G', "01111/10000/10000/10111/10001/10001/01111"),
            Map.entry('H', "10001/10001/10001/11111/10001/10001/10001"),
            Map.entry('I', "01110/00100/00100/00100/00100/00100/01110"),
            Map.entry('J', "00111/00010/00010/00010/00010/10010/01100"),
            Map.entry('K', "10001/10010/10100/11000/10100/10010/10001"),
            Map.entry('L', "10000/10000/10000/10000/10000/10000/11111"),
            Map.entry('M', "10001/11011/10101/10101/10001/10001/10001"),
            Map.entry('N', "10001/11001/10101/10011/10001/10001/10001"),
            Map.entry('O', "01110/10001/10001/10001/10001/10001/01110"),
            Map.entry('P', "11110/10001/10001/11110/10000/10000/10000"),
            Map.entry('Q', "01110/10001/10001/10001/10101/10010/01101"),
            Map.entry('R', "11110/10001/10001/11110/10100/10010/10001"),
            Map.entry('S', "01111/10000/10000/01110/00001/00001/11110"),
            Map.entry('T', "11111/00100/00100/00100/00100/00100/00100"),
            Map.entry('U', "10001/10001/10001/10001/10001/10001/01110"),
            Map.entry('V', "10001/10001/10001/10001/10001/01010/00100"),
            Map.entry('W', "10001/10001/10001/10101/10101/11011/10001"),
            Map.entry('X', "10001/10001/01010/00100/01010/10001/10001"),
            Map.entry('Y', "10001/10001/01010/00100/00100/00100/00100"),
            Map.entry('Z', "11111/00001/00010/00100/01000/10000/11111"),
            Map.entry('0', "01110/10001/10011/10101/11001/10001/01110"),
            Map.entry('1', "00100/01100/00100/00100/00100/00100/01110"),
            Map.entry('2', "01110/10001/00001/00010/00100/01000/11111"),
            Map.entry('3', "11110/00001/00001/01110/00001/00001/11110"),
            Map.entry('4', "00010/00110/01010/10010/11111/00010/00010"),
            Map.entry('5', "11111/10000/10000/11110/00001/00001/11110"),
            Map.entry('6', "01110/10000/10000/11110/10001/10001/01110"),
            Map.entry('7', "11111/00001/00010/00100/01000/01000/01000"),
            Map.entry('8', "01110/10001/10001/01110/10001/10001/01110"),
            Map.entry('9', "01110/10001/10001/01111/00001/00001/01110"),
            Map.entry('+', "00000/00100/00100/11111/00100/00100/00000"),
            Map.entry('-', "00000/00000/00000/11111/00000/00000/00000"),
            Map.entry('×', "10001/10001/01010/00100/01010/10001/10001"),
            Map.entry('÷', "00000/00100/00000/11111/00000/00100/00000"),
            Map.entry('=', "00000/00000/11111/00000/11111/00000/00000"),
            Map.entry('?', "01110/10001/00001/00010/00100/00000/00100")
    );

    private final int width;
    private final int height;

    public CaptchaRenderer(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Captcha dimensions must be positive");
        }
        this.width = width;
        this.height = height;
    }

    /** 渲染英文字母、数字和算术验证码；空格省略以保证小尺寸算式的可读性。 */
    public byte[] render(String text) {
        final String symbols = text.replace(" ", "");
        final int[] pixels = new int[Math.multiplyExact(width, height)];
        Arrays.fill(pixels, 0xffffff);
        IntStream.range(0, symbols.length())
                .forEach(index -> drawGlyph(pixels, symbols.charAt(index), index, symbols.length()));
        drawNoise(pixels);
        return encodePng(pixels);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void drawGlyph(int[] pixels, char character, int index, int count) {
        final String pattern = GLYPHS.get(Character.toUpperCase(character));
        if (pattern == null) {
            throw new IllegalArgumentException("Unsupported captcha character: " + character);
        }
        final int cellWidth = width / count;
        final int glyphWidth = Math.max(1, Math.min(cellWidth - 4, height * 5 / 9));
        final int glyphHeight = Math.max(1, Math.min(height - 12, glyphWidth * 7 / 5));
        final int left = index * cellWidth + (cellWidth - glyphWidth) / 2 + RANDOM.nextInt(3) - 1;
        final int top = (height - glyphHeight) / 2 + RANDOM.nextInt(7) - 3;
        final int skew = RANDOM.nextInt(3) - 1;
        final int color = randomColor(0, 96);
        for (int y = 0; y < glyphHeight; y++) {
            final int row = y * 7 / glyphHeight;
            for (int x = 0; x < glyphWidth; x++) {
                if (pattern.charAt(row * 6 + x * 5 / glyphWidth) == '1') {
                    setPixel(pixels, left + x + (row - 3) * skew / 2, top + y, color);
                }
            }
        }
    }

    private void drawNoise(int[] pixels) {
        for (int line = 0; line < 4; line++) {
            final int startX = RANDOM.nextInt(width);
            final int startY = RANDOM.nextInt(height);
            final int deltaX = RANDOM.nextInt(width) - startX;
            final int deltaY = RANDOM.nextInt(height) - startY;
            final int steps = Math.max(1, Math.max(Math.abs(deltaX), Math.abs(deltaY)));
            final int color = randomColor(140, 90);
            IntStream.rangeClosed(0, steps).forEach(step -> setPixel(pixels,
                    startX + deltaX * step / steps, startY + deltaY * step / steps, color));
        }
        IntStream.range(0, 80).forEach(ignored -> setPixel(pixels,
                RANDOM.nextInt(width), RANDOM.nextInt(height), randomColor(160, 70)));
    }

    private void setPixel(int[] pixels, int x, int y, int color) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            pixels[y * width + x] = color;
        }
    }

    private static int randomColor(int minimum, int range) {
        return ((minimum + RANDOM.nextInt(range)) << 16)
                | ((minimum + RANDOM.nextInt(range)) << 8) | (minimum + RANDOM.nextInt(range));
    }

    private byte[] encodePng(int[] pixels) {
        final var bytes = new ByteArrayOutputStream();
        try (final var output = new DataOutputStream(bytes)) {
            output.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            final byte[] header = ByteBuffer.allocate(13).putInt(width).putInt(height)
                    .put(new byte[] {8, 2, 0, 0, 0}).array();
            writeChunk(output, "IHDR", header);
            writeChunk(output, "IDAT", compressPixels(pixels));
            writeChunk(output, "IEND", new byte[0]);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to encode captcha PNG", exception);
        }
    }

    private byte[] compressPixels(int[] pixels) throws IOException {
        final var compressed = new ByteArrayOutputStream();
        try (final var deflater = new DeflaterOutputStream(compressed)) {
            final byte[] scanline = new byte[1 + width * 3];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int color = pixels[y * width + x];
                    scanline[1 + x * 3] = (byte) (color >>> 16);
                    scanline[2 + x * 3] = (byte) (color >>> 8);
                    scanline[3 + x * 3] = (byte) color;
                }
                deflater.write(scanline);
            }
        }
        return compressed.toByteArray();
    }

    private static void writeChunk(DataOutputStream output, String type, byte[] content) throws IOException {
        final byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        final var checksum = new CRC32();
        checksum.update(name);
        checksum.update(content);
        output.writeInt(content.length);
        output.write(name);
        output.write(content);
        output.writeInt((int) checksum.getValue());
    }
}
