package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.service.CaptchaService;
import com.blog.util.CaptchaUtil;
import com.blog.vo.CaptchaVO;
import com.blog.vo.SliderCaptchaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate redisTemplate;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String SLIDER_CAPTCHA_PREFIX = "captcha:slider:";
    private static final String SLIDER_CAPTCHA_TOKEN_PREFIX = "captcha:slider:token:";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final int SLIDER_TRACK_WIDTH = 320;
    private static final int SLIDER_WIDTH = 46;
    private static final int SLIDER_TOLERANCE = 6;

    @Override
    public CaptchaVO generateCaptcha() {
        CaptchaUtil.CaptchaResult result = CaptchaUtil.generate();
        String captchaKey = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 存入 Redis，key=captcha:{captchaKey}，value=验证码（忽略大小写）
        redisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaKey,
                result.code().toLowerCase(),
                CAPTCHA_TTL
        );

        return CaptchaVO.builder()
                .captchaKey(captchaKey)
                .captchaImage(result.base64Image())
                .build();
    }

    @Override
    public void verifyCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }

        String redisKey = CAPTCHA_PREFIX + captchaKey;
        String cachedCode = redisTemplate.opsForValue().get(redisKey);

        try {
            if (cachedCode == null) {
                throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
            }
            if (!cachedCode.equals(captchaCode.toLowerCase())) {
                throw new BusinessException(ResultCode.CAPTCHA_ERROR);
            }
        } finally {
            // 无论校验成功或失败，都删除验证码（一次性使用）
            redisTemplate.delete(redisKey);
        }
    }

    @Override
    public SliderCaptchaVO generateSliderCaptcha() {
        String captchaKey = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        redisTemplate.opsForValue().set(SLIDER_CAPTCHA_PREFIX + captchaKey, "pending", CAPTCHA_TTL);

        return SliderCaptchaVO.builder()
                .captchaKey(captchaKey)
                .trackWidth(SLIDER_TRACK_WIDTH)
                .sliderWidth(SLIDER_WIDTH)
                .expiresInSeconds((int) CAPTCHA_TTL.toSeconds())
                .build();
    }

    @Override
    public SliderCaptchaVO verifySliderCaptcha(String captchaKey, Integer sliderX) {
        if (captchaKey == null || sliderX == null) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }

        String redisKey = SLIDER_CAPTCHA_PREFIX + captchaKey;
        String cached = redisTemplate.opsForValue().get(redisKey);
        try {
            if (cached == null) {
                throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
            }

            int expectedX = SLIDER_TRACK_WIDTH - SLIDER_WIDTH;
            if (Math.abs(sliderX - expectedX) > SLIDER_TOLERANCE) {
                throw new BusinessException(ResultCode.CAPTCHA_ERROR);
            }

            String token = UUID.randomUUID().toString().replace("-", "");
            redisTemplate.opsForValue().set(SLIDER_CAPTCHA_TOKEN_PREFIX + captchaKey, token, CAPTCHA_TTL);

            return SliderCaptchaVO.builder()
                    .captchaKey(captchaKey)
                    .trackWidth(SLIDER_TRACK_WIDTH)
                    .sliderWidth(SLIDER_WIDTH)
                    .expiresInSeconds((int) CAPTCHA_TTL.toSeconds())
                    .captchaToken(token)
                    .build();
        } finally {
            redisTemplate.delete(redisKey);
        }
    }

    @Override
    public void verifySliderCaptchaToken(String captchaKey, String captchaToken) {
        if (captchaKey == null || captchaToken == null) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }

        String redisKey = SLIDER_CAPTCHA_TOKEN_PREFIX + captchaKey;
        String cachedToken = redisTemplate.opsForValue().get(redisKey);
        try {
            if (cachedToken == null) {
                throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
            }
            if (!cachedToken.equals(captchaToken)) {
                throw new BusinessException(ResultCode.CAPTCHA_ERROR);
            }
        } finally {
            redisTemplate.delete(redisKey);
        }
    }
}
