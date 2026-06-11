package com.blog.service;

import com.blog.common.PageResult;
import com.blog.dto.CommentCreateDTO;
import com.blog.vo.CommentVO;

import java.util.List;

public interface CommentService {

    CommentVO create(Long userId, CommentCreateDTO dto);

    List<CommentVO> listByPostId(Long postId);

    PageResult<CommentVO> listAdmin(int pageNum, int pageSize, Long postId, Integer status);

    void approve(Long id);

    void reject(Long id);

    void delete(Long id);
}
