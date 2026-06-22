package com.blog.mapper;

import com.blog.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface TagMapper {

    @Select("SELECT * FROM t_tag WHERE id = #{id}")
    Tag findById(Long id);

    @Select("SELECT * FROM t_tag WHERE name = #{name}")
    Tag findByName(String name);

    @Select("SELECT * FROM t_tag ORDER BY id ASC")
    List<Tag> findAll();

    @Select("SELECT * FROM t_tag WHERE id IN (SELECT tag_id FROM t_post_tag WHERE post_id = #{postId})")
    List<Tag> findByPostId(Long postId);

    @Select("<script>" +
            "SELECT t.*, pt.post_id AS post_id FROM t_tag t " +
            "INNER JOIN t_post_tag pt ON t.id = pt.tag_id " +
            "WHERE pt.post_id IN <foreach collection='postIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach>" +
            "</script>")
    List<Tag> findByPostIds(@Param("postIds") List<Long> postIds);

    @Select("<script>" +
            "SELECT * FROM t_tag " +
            "<where>" +
            "<if test=\"keyword != null and keyword != ''\">" +
            "AND name LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "</where>" +
            "ORDER BY id ASC" +
            "</script>")
    List<Tag> search(@Param("keyword") String keyword);

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

    @Select("SELECT COUNT(*) FROM t_post_tag pt INNER JOIN t_post p ON pt.post_id = p.id WHERE pt.tag_id = #{tagId} AND p.status = 1")
    int countPublishedPostsByTagId(Long tagId);
}
