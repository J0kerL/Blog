package com.blog.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface PostTagMapper {

    @Insert("INSERT INTO t_post_tag (post_id, tag_id) VALUES (#{postId}, #{tagId})")
    int insert(@Param("postId") Long postId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM t_post_tag WHERE post_id = #{postId}")
    int deleteByPostId(Long postId);
}
