package com.github.zeng.alt.captcha.producer;

import com.github.zeng.alt.captcha.core.CaptchaRenderer;
import com.github.zeng.alt.captcha.model.CaptchaChallenge;
import com.github.zeng.alt.captcha.model.CaptchaType;

import java.security.SecureRandom;

public class ArithmeticCaptchaProducer implements CaptchaProducer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaptchaRenderer renderer;

    public ArithmeticCaptchaProducer(CaptchaRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.ARITHMETIC;
    }

    @Override
    public CaptchaChallenge produce() {
        int a = RANDOM.nextInt(50) + 1;
        int b = RANDOM.nextInt(50) + 1;
        int op = RANDOM.nextInt(4);

        String expression;
        int result;

        switch (op) {
            case 0 -> {
                expression = a + " + " + b + " = ?";
                result = a + b;
            }
            case 1 -> {
                expression = a + " - " + b + " = ?";
                result = a - b;
            }
            case 2 -> {
                expression = a + " \u00D7 " + b + " = ?";
                result = a * b;
            }
            default -> {
                b = Math.max(b, 1);
                a = (a / b) * b;
                a = Math.max(a, b);
                expression = a + " \u00F7 " + b + " = ?";
                result = a / b;
            }
        }

        byte[] imageBytes = renderer.render(expression);
        return new CaptchaChallenge(String.valueOf(result), imageBytes, expression, 300);
    }
}
