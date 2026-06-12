package com.blog.service;

import com.blog.common.PageResult;
import com.blog.dto.CommentCreateDTO;
import com.blog.vo.CommentVO;

import java.util.List;

public interface CommentService {

    CommentVO create(Long userId, CommentCreateDTO dto);

    List<CommentVO> listByPostId(Long postId);

    PageResult<CommentVO> listAdmin(int pageNum, int pageSize, Long postId, Integer status, String keyword);

    void approve(Long id);

    void reject(Long id);

    /** AI 审核单条评论：检测恶意内容，通过则 status=1，拒绝则 status=2 */
    void aiReview(Long id);

    void delete(Long id);
}
