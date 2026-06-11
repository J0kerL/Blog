package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Category;
import com.blog.mapper.CategoryMapper;
import com.blog.service.CategoryService;
import com.blog.util.SlugUtil;
import com.blog.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listAll() {
        return categoryMapper.findAll().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> search(String keyword) {
        return categoryMapper.search(keyword).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryVO create(CategoryDTO dto) {
        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        
        // 自动生成slug：如果用户未提供，则基于分类名称生成
        if (category.getSlug() == null || category.getSlug().isBlank()) {
            category.setSlug(SlugUtil.generateSlug(dto.getName()));
        }
        
        // 检查slug唯一性
        if (isSlugExists(category.getSlug(), null)) {
            throw new BusinessException("Slug已存在，请使用不同的Slug");
        }
        
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        categoryMapper.insert(category);
        return toVO(category);
    }

    @Override
    @Transactional
    public CategoryVO update(Long id, CategoryDTO dto) {
        if (categoryMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
        }
        
        // 如果提供了slug，检查唯一性（排除当前分类）
        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            if (isSlugExists(dto.getSlug(), id)) {
                throw new BusinessException("Slug已存在，请使用不同的Slug");
            }
        }
        
        // 动态更新：仅 DTO 中非 null 的字段会被写入数据库
        Category updateParam = new Category();
        updateParam.setId(id);
        updateParam.setName(dto.getName());
        updateParam.setSlug(dto.getSlug());
        updateParam.setDescription(dto.getDescription());
        updateParam.setSortOrder(dto.getSortOrder());
        categoryMapper.updateSelective(updateParam);
        return toVO(categoryMapper.findById(id));
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
    }

    private CategoryVO toVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        vo.setPostCount(categoryMapper.countPostsByCategoryId(category.getId()));
        return vo;
    }
    
    /**
     * 检查slug是否已存在
     * @param slug 要检查的slug
     * @param excludeId 排除的分类ID（用于更新时排除自身）
     * @return 是否存在
     */
    private boolean isSlugExists(String slug, Long excludeId) {
        Category existing = categoryMapper.findBySlug(slug);
        if (existing == null) {
            return false;
        }
        // 如果是更新操作，排除自身
        return excludeId == null || !existing.getId().equals(excludeId);
    }
}
