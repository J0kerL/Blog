package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.service.CaptchaService;
import com.blog.util.CaptchaUtil;
import com.blog.vo.CaptchaVO;
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
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

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
}
