package com.blog.util;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的频率限制工具类
 *
 * <p>使用滑动窗口计数器算法实现接口限流，防止恶意请求或接口被滥用。</p>
 *
 * <p>算法说明：</p>
 * <ul>
 *   <li>使用 Redis 的 INCR 命令实现原子计数</li>
 *   <li>首次请求时设置过期时间作为时间窗口</li>
 *   <li>窗口内计数超过阈值时抛出业务异常</li>
 *   <li>窗口过期后自动重置计数</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Autowired
 * private RateLimitUtil rateLimitUtil;
 *
 * // 限制用户每分钟最多 20 次 AI 调用
 * String key = "rate:ai:" + userId;
 * rateLimitUtil.checkRateLimit(key, 20, Duration.ofMinutes(1));
 * }</pre>
 *
 * @author Diamond
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class RateLimitUtil {

    /** Redis 模板，用于计数和设置过期时间 */
    private final StringRedisTemplate redisTemplate;

    /**
     * 检查频率限制，超出则抛出异常
     *
     * <p>实现逻辑：</p>
     * <ol>
     *   <li>对指定 key 执行原子递增</li>
     *   <li>如果是首次请求（count == 1），设置 key 的过期时间</li>
     *   <li>如果计数超过最大允许次数，抛出 {@link ResultCode#RATE_LIMIT_EXCEEDED} 异常</li>
     * </ol>
     *
     * @param key      限流 key，建议格式："rate:{业务}:{用户ID}"，如 "rate:ai:123"
     * @param maxCount 时间窗口内最大允许的请求数
     * @param window   时间窗口时长，如 Duration.ofMinutes(1) 表示 1 分钟
     * @throws BusinessException 当请求次数超过限制时抛出
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
