package com.blog.util;

import cn.hutool.captcha.LineCaptcha;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 图形验证码工具：基于 Hutool Captcha
 */
public final class CaptchaUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LEN = 4;
    private static final int LINE_COUNT = 60; // 干扰线数量

    private CaptchaUtil() {}

    public record CaptchaResult(String code, String base64Image) {}

    public static CaptchaResult generate() {
        // Hutool 一行生成线段干扰验证码
        LineCaptcha captcha = cn.hutool.captcha.CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, CODE_LEN, LINE_COUNT);
        String code = captcha.getCode();
        String base64 = toBase64(captcha.getImage());
        return new CaptchaResult(code, "data:image/png;base64," + base64);
    }

    private static String toBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
