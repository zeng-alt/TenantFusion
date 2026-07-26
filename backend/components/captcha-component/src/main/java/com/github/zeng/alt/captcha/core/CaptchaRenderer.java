package com.github.zeng.alt.captcha.core;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;

public final class CaptchaRenderer {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String[] FONT_NAMES = {"SansSerif", "Serif", "Monospaced"};

    private final int width;
    private final int height;

    public CaptchaRenderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public byte[] render(String text) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        drawInterferenceLines(g2d, width, height);
        drawNoise(g2d, width, height);

        int charWidth = width / Math.max(text.length(), 1);
        int fontSize = Math.min(height - 12, charWidth + 2);
        Font font = new Font(FONT_NAMES[RANDOM.nextInt(FONT_NAMES.length)], Font.BOLD, fontSize);
        g2d.setFont(font);

        for (int i = 0; i < text.length(); i++) {
            Color color = new Color(
                    RANDOM.nextInt(120),
                    RANDOM.nextInt(120),
                    RANDOM.nextInt(120)
            );
            g2d.setColor(color);

            AffineTransform old = g2d.getTransform();
            double angle = (RANDOM.nextDouble() - 0.5) * 0.6;
            int x = i * charWidth + charWidth / 4 + RANDOM.nextInt(5);
            int y = height - 10 - RANDOM.nextInt(8);
            g2d.rotate(angle, x, y);
            g2d.drawString(String.valueOf(text.charAt(i)), x, y);
            g2d.setTransform(old);
        }

        g2d.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render captcha image", e);
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    private static void drawInterferenceLines(Graphics2D g2d, int w, int h) {
        g2d.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 6; i++) {
            g2d.setColor(new Color(
                    RANDOM.nextInt(180) + 50,
                    RANDOM.nextInt(180) + 50,
                    RANDOM.nextInt(180) + 50
            ));
            int x1 = RANDOM.nextInt(w);
            int y1 = RANDOM.nextInt(h);
            int x2 = RANDOM.nextInt(w);
            int y2 = RANDOM.nextInt(h);
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    private static void drawNoise(Graphics2D g2d, int w, int h) {
        for (int i = 0; i < 80; i++) {
            g2d.setColor(new Color(
                    RANDOM.nextInt(200) + 30,
                    RANDOM.nextInt(200) + 30,
                    RANDOM.nextInt(200) + 30
            ));
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            g2d.drawRect(x, y, 1, 1);
        }
    }
}
