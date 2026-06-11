package com.blog.mapper;

import com.blog.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface TagMapper {

    @Select("SELECT * FROM t_tag WHERE id = #{id}")
    Tag findById(Long id);

    @Select("SELECT * FROM t_tag ORDER BY id ASC")
    List<Tag> findAll();

    @Select("SELECT * FROM t_tag WHERE id IN (SELECT tag_id FROM t_post_tag WHERE post_id = #{postId})")
    List<Tag> findByPostId(Long postId);

    @Insert("INSERT INTO t_tag (name, slug, created_at) VALUES (#{name}, #{slug}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    @Update("UPDATE t_tag SET name=#{name}, slug=#{slug} WHERE id=#{id}")
    int update(Tag tag);

    /** 动态更新：仅更新非 null 字段（XML 实现） */
    int updateSelective(Tag tag);

    @Delete("DELETE FROM t_tag WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM t_post_tag WHERE tag_id = #{tagId}")
    int countPostsByTagId(Long tagId);
}
