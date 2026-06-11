package com.blog.util;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的简单频率限制工具（滑动窗口计数器）
 */
@Component
@RequiredArgsConstructor
public class RateLimitUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查频率限制，超出则抛出异常
     *
     * @param key      限流 key（如 "rate:ai:userId"）
     * @param maxCount 窗口内最大请求数
     * @param window   时间窗口
     */
    public void checkRateLimit(String key, long maxCount, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        if (count != null && count > maxCount) {
            throw new BusinessException(ResultCode.RATE_LIMIT_EXCEEDED);
        }
    }
}
