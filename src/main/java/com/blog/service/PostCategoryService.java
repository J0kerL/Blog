package com.blog.service;

import java.util.List;

/**
 * 文章分类标签关联服务接口
 *
 * <p>负责处理文章与分类、标签的关联关系，包括自动创建不存在的分类和标签。</p>
 *
 * @author Diamond
 * @since 1.0.0
 */
public interface PostCategoryService {

    /**
     * 解析并合并分类 ID 列表
     *
     * <p>将已有的分类 ID 列表和新建分类名称列表合并，自动创建不存在的分类。</p>
     *
     * @param existingIds 已有的分类 ID 列表，可以为 null
     * @param newNames    新建分类名称列表，可以为 null
     * @return 合并后的分类 ID 列表
     */
    List<Long> resolveCategoryIds(List<Long> existingIds, List<String> newNames);

    /**
     * 解析并合并标签 ID 列表
     *
     * <p>将已有的标签 ID 列表和新建标签名称列表合并，自动创建不存在的标签。</p>
     *
     * @param existingIds 已有的标签 ID 列表，可以为 null
     * @param newNames    新建标签名称列表，可以为 null
     * @return 合并后的标签 ID 列表
     */
    List<Long> resolveTagIds(List<Long> existingIds, List<String> newNames);

    /**
     * 关联文章与分类
     *
     * @param postId      文章 ID
     * @param categoryIds 分类 ID 列表
     */
    void associateCategories(Long postId, List<Long> categoryIds);

    /**
     * 关联文章与标签
     *
     * @param postId 文章 ID
     * @param tagIds 标签 ID 列表
     */
    void associateTags(Long postId, List<Long> tagIds);

    /**
     * 删除文章的所有分类关联
     *
     * @param postId 文章 ID
     */
    void deleteCategoryAssociations(Long postId);

    /**
     * 删除文章的所有标签关联
     *
     * @param postId 文章 ID
     */
    void deleteTagAssociations(Long postId);
}
