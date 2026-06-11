package com.blog.mapper;

import com.blog.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface PostCategoryMapper {

    @Insert("INSERT INTO t_post_category (post_id, category_id) VALUES (#{postId}, #{categoryId})")
    int insert(@Param("postId") Long postId, @Param("categoryId") Long categoryId);

    @Delete("DELETE FROM t_post_category WHERE post_id = #{postId}")
    int deleteByPostId(Long postId);

    @Select("SELECT c.* FROM t_category c INNER JOIN t_post_category pc ON c.id = pc.category_id WHERE pc.post_id = #{postId}")
    List<Category> findCategoriesByPostId(Long postId);

    @Select("<script>" +
            "SELECT c.*, pc.post_id AS post_id FROM t_category c " +
            "INNER JOIN t_post_category pc ON c.id = pc.category_id " +
            "WHERE pc.post_id IN <foreach collection='postIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach>" +
            "</script>")
    List<Category> findCategoriesByPostIds(@Param("postIds") List<Long> postIds);
}
