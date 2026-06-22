package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Category;
import com.blog.mapper.CategoryMapper;
import com.blog.converter.EntityConverter;
import com.blog.service.CategoryService;
import com.blog.util.SlugUtil;
import com.blog.vo.CategoryVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final EntityConverter entityConverter;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 分类列表缓存 Key */
    private static final String CACHE_KEY_ALL = "cache:categories:all";
    private static final String CACHE_KEY_PUBLISHED = "cache:categories:published";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    @Transactional(readOnly = true)
    public List<CategoryVO> listAll() {
        // 尝试从缓存获取
        String cached = redisTemplate.opsForValue().get(CACHE_KEY_ALL);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<CategoryVO>>() {});
            } catch (Exception e) {
                log.warn("反序列化分类缓存失败", e);
            }
        }

        // 从数据库查询
        List<CategoryVO> result = categoryMapper.findAll().stream()
                .map(this::toVOWithPostCount)
                .collect(Collectors.toList());

        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(CACHE_KEY_ALL, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("序列化分类缓存失败", e);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryVO> listPublished() {
        // 尝试从缓存获取
        String cached = redisTemplate.opsForValue().get(CACHE_KEY_PUBLISHED);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<CategoryVO>>() {});
            } catch (Exception e) {
                log.warn("反序列化分类缓存失败", e);
            }
        }

        // 从数据库查询
        List<CategoryVO> result = categoryMapper.findAll().stream()
                .map(this::toVOWithPublishedCount)
                .collect(Collectors.toList());

        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(CACHE_KEY_PUBLISHED, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("序列化分类缓存失败", e);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryVO> search(String keyword) {
        return categoryMapper.search(keyword).stream().map(this::toVOWithPostCount).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryVO create(CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder());

        if (category.getSlug() == null || category.getSlug().isBlank()) {
            category.setSlug(SlugUtil.generateSlug(dto.getName()));
        }

        if (isSlugExists(category.getSlug(), null)) {
            throw new BusinessException("Slug已存在，请使用不同的Slug");
        }

        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        categoryMapper.insert(category);
        
        // 清除缓存
        clearCache();
        
        return entityConverter.toCategoryVO(category);
    }

    @Override
    @Transactional
    public CategoryVO update(Long id, CategoryDTO dto) {
        if (categoryMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }

        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            if (isSlugExists(dto.getSlug(), id)) {
                throw new BusinessException("Slug已存在，请使用不同的Slug");
            }
        }

        Category updateParam = new Category();
        updateParam.setId(id);
        updateParam.setName(dto.getName());
        updateParam.setSlug(dto.getSlug());
        updateParam.setDescription(dto.getDescription());
        updateParam.setSortOrder(dto.getSortOrder());
        categoryMapper.updateSelective(updateParam);

        Category updated = categoryMapper.findById(id);
        
        // 清除缓存
        clearCache();
        
        return entityConverter.toCategoryVO(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (categoryMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        if (categoryMapper.countPostsByCategoryId(id) > 0) {
            throw new BusinessException("该分类下仍有文章，无法删除");
        }
        categoryMapper.deleteById(id);
        
        // 清除缓存
        clearCache();
    }

    private boolean isSlugExists(String slug, Long excludeId) {
        Category existing = categoryMapper.findBySlug(slug);
        if (existing == null) {
            return false;
        }
        return excludeId == null || !existing.getId().equals(excludeId);
    }

    private CategoryVO toVOWithPostCount(Category category) {
        CategoryVO vo = entityConverter.toCategoryVO(category);
        vo.setPostCount(categoryMapper.countPostsByCategoryId(category.getId()));
        return vo;
    }

    private CategoryVO toVOWithPublishedCount(Category category) {
        CategoryVO vo = entityConverter.toCategoryVO(category);
        vo.setPostCount(categoryMapper.countPublishedPostsByCategoryId(category.getId()));
        return vo;
    }

    /**
     * 清除分类缓存
     */
    private void clearCache() {
        redisTemplate.delete(CACHE_KEY_ALL);
        redisTemplate.delete(CACHE_KEY_PUBLISHED);
        log.debug("分类缓存已清除");
    }
}
