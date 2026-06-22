package com.blog.mapper;

import com.blog.entity.Post;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PostMapper {

    Post findById(@Param("id") Long id);

    Post findBySlug(@Param("slug") String slug);

    int insert(Post post);

    int update(Post post);

    int deleteById(@Param("id") Long id);

    int incrementViewCount(@Param("id") Long id);

    /**
     * 前台查询已发布文章列表（支持分类/标签/搜索过滤）
     */
    List<Post> findPublishedList(@Param("keyword") String keyword,
                                 @Param("categoryId") Long categoryId,
                                 @Param("tagId") Long tagId);

    /**
     * Admin 查询全部文章列表
     */
    List<Post> findAdminList(@Param("keyword") String keyword,
                             @Param("status") Integer status,
                             @Param("categoryId") Long categoryId);

    /**
     * 查询指定用户的文章列表
     */
    List<Post> findByUserId(@Param("userId") Long userId,
                            @Param("keyword") String keyword,
                            @Param("status") Integer status);
}
