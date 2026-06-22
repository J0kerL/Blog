package com.blog.service;

/**
 * 文章视图计数服务接口
 *
 * <p>使用 Redis 缓存文章的阅读量，减少数据库压力。通过定时任务批量同步到数据库。</p>
 *
 * @author Diamond
 * @since 1.0.0
 */
public interface ViewCountService {

    /**
     * 增加文章的阅读量（Redis 缓存）
     *
     * <p>使用 Redis INCR 命令原子性地增加计数，不直接操作数据库。</p>
     *
     * @param postId 文章 ID
     */
    void incrementViewCount(Long postId);

    /**
     * 获取文章的阅读量（数据库 + Redis 增量）
     *
     * <p>返回值 = 数据库存储的基础值 + Redis 中累积的增量</p>
     *
     * @param postId 文章 ID
     * @param dbViewCount 数据库中的阅读量
     * @return 合并后的总阅读量
     */
    Long getViewCount(Long postId, Long dbViewCount);

    /**
     * 将 Redis 中累积的阅读量同步到数据库
     *
     * <p>由定时任务调用，执行以下操作：</p>
     * <ol>
     *   <li>获取所有 view:count:* 的键</li>
     *   <li>读取并删除这些键（原子操作）</li>
     *   <li>批量更新数据库</li>
     * </ol>
     *
     * @return 同步的文章数量
     */
    int syncViewCountToDatabase();
}
