package com.blog.service;

import com.blog.common.PageResult;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.ForgotPasswordDTO;
import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.vo.AdminUserVO;
import com.blog.vo.LoginVO;
import com.blog.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    LoginVO register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    UserVO getProfile(Long userId);

    UserVO updateProfile(Long userId, UserUpdateDTO dto);

    /** 上传头像：文件 → OSS → 更新数据库 → 返回 URL */
    String uploadAvatar(Long userId, MultipartFile file);

    /** 修改密码（已登录用户） */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /** 忘记密码（未登录，需验证码 + 邮箱） */
    void forgotPassword(ForgotPasswordDTO dto);

    void logout();

    // ========== Admin ==========

    /** Admin 用户列表 */
    PageResult<AdminUserVO> listUsers(int pageNum, int pageSize, String keyword, String role, Integer status);

    /** Admin 更新用户状态（启用/禁用） */
    void updateUserStatus(Long userId, Integer status);

    /** Admin 更新用户角色 */
    void updateUserRole(Long userId, String role);
}
