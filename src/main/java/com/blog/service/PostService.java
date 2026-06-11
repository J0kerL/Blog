package com.blog.service;

import com.blog.common.PageResult;
import com.blog.dto.PostCreateDTO;
import com.blog.vo.PostListVO;
import com.blog.vo.PostVO;

public interface PostService {

    PostVO create(Long userId, PostCreateDTO dto);

    PostVO update(Long postId, PostCreateDTO dto);

    PostVO updateStatus(Long postId, Integer status);

    void delete(Long postId);

    PostVO getById(Long id);

    PostVO getByIdForView(Long id);

    PostVO getBySlug(String slug);

    PageResult<PostListVO> listPublished(int pageNum, int pageSize, String keyword, Long categoryId, Long tagId);

    PageResult<PostListVO> listAdmin(int pageNum, int pageSize, String keyword, Integer status, Long categoryId);
}
