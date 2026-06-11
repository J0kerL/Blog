package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Category;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.convert.EntityConverter;
import com.blog.service.CategoryService;
import com.blog.util.SlugUtil;
import com.blog.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final EntityConverter entityConverter;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryVO> listAll() {
        return categoryMapper.findAll().stream().map(this::toVOWithPostCount).collect(Collectors.toList());
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
}
