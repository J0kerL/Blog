package com.blog.util;

import cn.hutool.captcha.LineCaptcha;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 图形验证码生成工具类
 *
 * <p>基于 Hutool Captcha 库生成带有干扰线的图形验证码，用于登录、注册等场景的人机验证。</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>生成 4 位随机验证码</li>
 *   <li>图片尺寸：120x40 像素</li>
 *   <li>包含 60 条干扰线，增加识别难度</li>
 *   <li>输出 Base64 编码的图片，可直接嵌入 HTML</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * CaptchaUtil.CaptchaResult result = CaptchaUtil.generate();
 * String code = result.code();        // 验证码文本，如 "aB3d"
 * String image = result.base64Image(); // Base64 图片，如 "data:image/png;base64,..."
 * }</pre>
 *
 * @author Diamond
 * @since 1.0.0
 */
public final class CaptchaUtil {

    /** 验证码图片宽度（像素） */
    private static final int WIDTH = 120;
    
    /** 验证码图片高度（像素） */
    private static final int HEIGHT = 40;
    
    /** 验证码字符长度 */
    private static final int CODE_LEN = 4;
    
    /** 干扰线数量，增加 OCR 识别难度 */
    private static final int LINE_COUNT = 60;

    private CaptchaUtil() {
        // 工具类不允许实例化
    }

    /**
     * 验证码结果记录类
     *
     * @param code       验证码文本（4位字符）
     * @param base64Image 验证码图片的 Base64 编码（含 data:image/png;base64, 前缀）
     */
    public record CaptchaResult(String code, String base64Image) {}

    /**
     * 生成图形验证码
     *
     * @return 包含验证码文本和 Base64 图片的结果对象
     */
    public static CaptchaResult generate() {
        // Hutool 一行生成线段干扰验证码
        LineCaptcha captcha = cn.hutool.captcha.CaptchaUtil.createLineCaptcha(WIDTH, HEIGHT, CODE_LEN, LINE_COUNT);
        String code = captcha.getCode();
        String base64 = toBase64(captcha.getImage());
        return new CaptchaResult(code, "data:image/png;base64," + base64);
    }

    /**
     * 将 BufferedImage 转换为 Base64 字符串
     *
     * @param image 待转换的图片
     * @return Base64 编码的字符串
     * @throws RuntimeException 当图片编码失败时抛出
     */
    private static String toBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
