package com.blog.task;

import com.blog.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 视图计数同步定时任务
 *
 * <p>定期将 Redis 中累积的文章阅读量同步到数据库，减少数据库压力。</p>
 *
 * <p>执行策略：</p>
 * <ul>
 *   <li>每 5 分钟执行一次</li>
 *   <li>读取 Redis 中所有 view:count:* 的键</li>
 *   <li>批量更新数据库</li>
 *   <li>同步完成后删除 Redis 键</li>
 * </ul>
 *
 * @author Diamond
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private final ViewCountService viewCountService;

    /**
     * 定时同步视图计数到数据库
     *
     * <p>每 5 分钟执行一次（300000 毫秒）</p>
     */
    @Scheduled(fixedRate = 300000) // 5 分钟
    public void syncViewCount() {
        try {
            log.debug("开始执行视图计数同步任务...");
            int count = viewCountService.syncViewCountToDatabase();
            if (count > 0) {
                log.info("视图计数同步任务完成，共同步 {} 条记录", count);
            }
        } catch (Exception e) {
            log.error("视图计数同步任务执行失败", e);
        }
    }
}
