package com.blog.mapper;

import com.blog.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UserMapper {

    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM t_user WHERE email = #{email}")
    User findByEmail(String email);

    @Select("<script>" +
            "SELECT * FROM t_user" +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (username LIKE CONCAT('%',#{keyword},'%') OR nickname LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='role != null and role != \"\"'>" +
            "  AND role = #{role}" +
            "</if>" +
            "<if test='status != null'>" +
            "  AND status = #{status}" +
            "</if>" +
            "</where>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<User> findAll(@Param("keyword") String keyword,
                       @Param("role") String role,
                       @Param("status") Integer status);

    @Insert("INSERT INTO t_user (username, password, nickname, email, role, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{email}, #{role}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE t_user SET nickname=#{nickname}, email=#{email}, avatar=#{avatar}, bio=#{bio}, updated_at=NOW() WHERE id=#{id}")
    int updateProfile(User user);

    /** 动态更新：仅更新非 null 字段（XML 实现） */
    int updateProfileSelective(User user);

    @Update("UPDATE t_user SET password=#{password}, updated_at=NOW() WHERE id=#{id}")
    int updatePassword(User user);

    @Update("UPDATE t_user SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE t_user SET role=#{role}, updated_at=NOW() WHERE id=#{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);
}
