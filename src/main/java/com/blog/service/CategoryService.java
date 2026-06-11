package com.blog.service;

import com.blog.dto.CategoryDTO;
import com.blog.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    List<CategoryVO> listAll();

    List<CategoryVO> search(String keyword);

    CategoryVO create(CategoryDTO dto);

    CategoryVO update(Long id, CategoryDTO dto);

    void delete(Long id);
}
