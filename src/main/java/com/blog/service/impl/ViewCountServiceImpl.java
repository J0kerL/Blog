package com.blog.service.impl;

import com.blog.mapper.PostMapper;
import com.blog.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 文章视图计数服务实现类
 *
 * <p>使用 Redis 缓存文章阅读量的增量，通过定时任务批量同步到数据库，减少数据库压力。</p>
 *
 * @author Diamond
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountServiceImpl implements ViewCountService {

    private final StringRedisTemplate redisTemplate;
    private final PostMapper postMapper;

    /** Redis key 前缀 */
    private static final String VIEW_COUNT_PREFIX = "view:count:";

    /**
     * 增加文章的阅读量（Redis 缓存）
     *
     * @param postId 文章 ID
     */
    @Override
    public void incrementViewCount(Long postId) {
        String key = VIEW_COUNT_PREFIX + postId;
        redisTemplate.opsForValue().increment(key);
    }

    /**
     * 获取文章的阅读量（数据库 + Redis 增量）
     *
     * @param postId      文章 ID
     * @param dbViewCount 数据库中的阅读量
     * @return 合并后的总阅读量
     */
    @Override
    public Long getViewCount(Long postId, Long dbViewCount) {
        String key = VIEW_COUNT_PREFIX + postId;
        String cached = redisTemplate.opsForValue().get(key);
        long redisIncrement = 0;
        if (cached != null) {
            try {
                redisIncrement = Long.parseLong(cached);
            } catch (NumberFormatException e) {
                log.warn("Redis 视图计数格式异常: key={}, value={}", key, cached);
            }
        }
        return (dbViewCount != null ? dbViewCount : 0) + redisIncrement;
    }

    /**
     * 将 Redis 中累积的阅读量同步到数据库
     *
     * @return 同步的文章数量
     */
    @Override
    public int syncViewCountToDatabase() {
        // 获取所有 view:count:* 的键
        Set<String> keys = redisTemplate.keys(VIEW_COUNT_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        log.info("开始同步视图计数到数据库，共 {} 个待同步", keys.size());

        int syncedCount = 0;
        for (String key : keys) {
            try {
                // 获取并删除键（原子操作）
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    long increment = Long.parseLong(value);
                    if (increment > 0) {
                        // 从 key 中提取 postId
                        Long postId = Long.parseLong(key.substring(VIEW_COUNT_PREFIX.length()));
                        // 更新数据库
                        postMapper.addViewCount(postId, increment);
                        // 删除 Redis 键
                        redisTemplate.delete(key);
                        syncedCount++;
                    }
                }
            } catch (Exception e) {
                log.error("同步视图计数失败: key={}", key, e);
            }
        }

        log.info("视图计数同步完成，共同步 {} 条", syncedCount);
        return syncedCount;
    }
}
