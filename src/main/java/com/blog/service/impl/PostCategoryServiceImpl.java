package com.blog.service.impl;

import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.PostCategoryMapper;
import com.blog.mapper.PostTagMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.PostCategoryService;
import com.blog.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章分类标签关联服务实现类
 *
 * @author Diamond
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostCategoryServiceImpl implements PostCategoryService {

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostCategoryMapper postCategoryMapper;
    private final PostTagMapper postTagMapper;

    /**
     * 解析并合并分类 ID 列表
     */
    @Override
    @Transactional
    public List<Long> resolveCategoryIds(List<Long> existingIds, List<String> newNames) {
        List<Long> result = new ArrayList<>();
        if (existingIds != null) {
            result.addAll(existingIds);
        }
        if (newNames != null) {
            for (String name : newNames) {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) continue;

                Category existing = categoryMapper.findByName(trimmed);
                if (existing != null) {
                    if (!result.contains(existing.getId())) {
                        result.add(existing.getId());
                    }
                } else {
                    // 自动创建新分类
                    Category cat = new Category();
                    cat.setName(trimmed);
                    cat.setSlug(SlugUtil.generateSlug(trimmed));
                    cat.setSortOrder(0);
                    categoryMapper.insert(cat);
                    result.add(cat.getId());
                    log.info("自动创建新分类: {}", trimmed);
                }
            }
        }
        return result;
    }

    /**
     * 解析并合并标签 ID 列表
     */
    @Override
    @Transactional
    public List<Long> resolveTagIds(List<Long> existingIds, List<String> newNames) {
        List<Long> result = new ArrayList<>();
        if (existingIds != null) {
            result.addAll(existingIds);
        }
        if (newNames != null) {
            for (String name : newNames) {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) continue;

                Tag existing = tagMapper.findByName(trimmed);
                if (existing != null) {
                    if (!result.contains(existing.getId())) {
                        result.add(existing.getId());
                    }
                } else {
                    // 自动创建新标签
                    Tag tag = new Tag();
                    tag.setName(trimmed);
                    tag.setSlug(SlugUtil.generateSlug(trimmed));
                    tagMapper.insert(tag);
                    result.add(tag.getId());
                    log.info("自动创建新标签: {}", trimmed);
                }
            }
        }
        return result;
    }

    /**
     * 关联文章与分类
     */
    @Override
    public void associateCategories(Long postId, List<Long> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long catId : categoryIds) {
                postCategoryMapper.insert(postId, catId);
            }
        }
    }

    /**
     * 关联文章与标签
     */
    @Override
    public void associateTags(Long postId, List<Long> tagIds) {
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                postTagMapper.insert(postId, tagId);
            }
        }
    }

    /**
     * 删除文章的所有分类关联
     */
    @Override
    public void deleteCategoryAssociations(Long postId) {
        postCategoryMapper.deleteByPostId(postId);
    }

    /**
     * 删除文章的所有标签关联
     */
    @Override
    public void deleteTagAssociations(Long postId) {
        postTagMapper.deleteByPostId(postId);
    }
}
