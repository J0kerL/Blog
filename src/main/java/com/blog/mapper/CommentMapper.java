package com.blog.mapper;

import com.blog.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentMapper {

    Comment findById(@Param("id") Long id);

    List<Comment> findByPostId(@Param("postId") Long postId, @Param("status") Integer status);

    List<Comment> findTopLevelByPostId(@Param("postId") Long postId);

    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    /** 查询某篇文章下所有已通过的回复（用于批量组装，消除 N+1） */
    List<Comment> findRepliesByPostId(@Param("postId") Long postId);

    int insert(Comment comment);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);

    List<Comment> findAdminList(@Param("postId") Long postId, @Param("status") Integer status);
}
