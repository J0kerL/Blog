package com.blog.mapper;

import com.blog.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CategoryMapper {

    @Select("SELECT * FROM t_category WHERE id = #{id}")
    Category findById(Long id);

    @Select("SELECT * FROM t_category WHERE slug = #{slug}")
    Category findBySlug(String slug);

    @Select("SELECT * FROM t_category WHERE name = #{name}")
    Category findByName(String name);

    @Select("SELECT * FROM t_category ORDER BY sort_order ASC, id ASC")
    List<Category> findAll();

    @Select("<script>" +
            "SELECT * FROM t_category " +
            "<where>" +
            "<if test=\"keyword != null and keyword != ''\">" +
            "AND (name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "</where>" +
            "ORDER BY sort_order ASC, id ASC" +
            "</script>")
    List<Category> search(@Param("keyword") String keyword);

    @Insert("INSERT INTO t_category (name, slug, description, sort_order, created_at) " +
            "VALUES (#{name}, #{slug}, #{description}, #{sortOrder}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Update("UPDATE t_category SET name=#{name}, slug=#{slug}, description=#{description}, sort_order=#{sortOrder} WHERE id=#{id}")
    int update(Category category);

    /** 动态更新：仅更新非 null 字段（XML 实现） */
    int updateSelective(Category category);

    @Delete("DELETE FROM t_category WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM t_post_category WHERE category_id = #{categoryId}")
    int countPostsByCategoryId(Long categoryId);

    @Select("SELECT COUNT(*) FROM t_post_category pc INNER JOIN t_post p ON pc.post_id = p.id WHERE pc.category_id = #{categoryId} AND p.status = 1")
    int countPublishedPostsByCategoryId(Long categoryId);
}
