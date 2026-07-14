package com.github.zeng.alt.captcha.producer;

import com.github.zeng.alt.captcha.model.CaptchaChallenge;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

public class ImageCaptchaProducer implements CaptchaProducer {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Font[] FONTS = {
            new Font("SansSerif", Font.BOLD, 36),
            new Font("Serif", Font.BOLD, 36),
            new Font("Monospaced", Font.BOLD, 36)
    };

    private final int width;
    private final int height;
    private final int length;

    public ImageCaptchaProducer(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
    }

    @Override
    public String type() {
        return "image";
    }

    @Override
    public CaptchaChallenge produce() {
        String code = generateCode();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        drawInterferenceLines(g2d);
        drawNoise(g2d);

        int charWidth = width / length;
        for (int i = 0; i < code.length(); i++) {
            g2d.setFont(FONTS[RANDOM.nextInt(FONTS.length)]);

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
            g2d.drawString(String.valueOf(code.charAt(i)), x, y);
            g2d.setTransform(old);
        }

        g2d.dispose();

        String base64;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }

        return new CaptchaChallenge(code, "data:image/png;base64," + base64, null, 300);
    }

    private void drawInterferenceLines(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 6; i++) {
            g2d.setColor(new Color(
                    RANDOM.nextInt(180) + 50,
                    RANDOM.nextInt(180) + 50,
                    RANDOM.nextInt(180) + 50
            ));
            int x1 = RANDOM.nextInt(width);
            int y1 = RANDOM.nextInt(height);
            int x2 = RANDOM.nextInt(width);
            int y2 = RANDOM.nextInt(height);
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawNoise(Graphics2D g2d) {
        for (int i = 0; i < 80; i++) {
            g2d.setColor(new Color(
                    RANDOM.nextInt(200) + 30,
                    RANDOM.nextInt(200) + 30,
                    RANDOM.nextInt(200) + 30
            ));
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            g2d.drawRect(x, y, 1, 1);
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }
}
