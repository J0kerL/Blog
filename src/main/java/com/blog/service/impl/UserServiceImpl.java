package com.blog.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.ForgotPasswordDTO;
import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.CaptchaService;
import com.blog.service.UserService;
import com.blog.util.OssUtil;
import com.blog.vo.AdminUserVO;
import com.blog.vo.LoginVO;
import com.blog.vo.UserVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

import static cn.dev33.satoken.secure.SaSecureUtil.md5;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final CaptchaService captchaService;
    private final OssUtil ossUtil;

    @Override
    public LoginVO register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(md5(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole("ROLE_USER");
        user.setStatus(1);
        userMapper.insert(user);

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        return LoginVO.builder()
                .token(token)
                .tokenPrefix("Bearer")
                .user(toUserVO(user))
                .build();
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        String encryptedPassword = md5(dto.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 业务字段校验通过后，最后校验验证码（一次性消耗）
        captchaService.verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        return LoginVO.builder()
                .token(token)
                .tokenPrefix("Bearer")
                .user(toUserVO(user))
                .build();
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, UserUpdateDTO dto) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 动态更新：仅 DTO 中非 null 的字段会被写入数据库
        User updateParam = new User();
        updateParam.setId(userId);
        updateParam.setNickname(dto.getNickname());
        updateParam.setEmail(dto.getEmail());
        updateParam.setBio(dto.getBio());
        userMapper.updateProfileSelective(updateParam);
        return getProfile(userId);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (userMapper.findById(userId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 校验文件类型和大小
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("头像图片大小不能超过 5MB");
        }
        // 上传到 OSS
        String avatarUrl = ossUtil.upload(file);
        // 更新数据库
        User updateParam = new User();
        updateParam.setId(userId);
        updateParam.setAvatar(avatarUrl);
        userMapper.updateProfileSelective(updateParam);
        return avatarUrl;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 校验旧密码
        String oldEncrypted = md5(dto.getOldPassword());
        if (!oldEncrypted.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        // 更新新密码
        String newEncrypted = md5(dto.getNewPassword());
        user.setPassword(newEncrypted);
        userMapper.updatePassword(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO dto) {
        // 先验证邮箱是否存在
        User user = userMapper.findByEmail(dto.getEmail());
        if (user == null) {
            throw new BusinessException(ResultCode.EMAIL_NOT_FOUND);
        }
        // 业务校验通过后，最后校验验证码（一次性消耗）
        captchaService.verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        // 更新密码
        String newEncrypted = md5(dto.getNewPassword());
        user.setPassword(newEncrypted);
        userMapper.updatePassword(user);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    // ========== Admin ==========

    @Override
    public PageResult<AdminUserVO> listUsers(int pageNum, int pageSize, String keyword, String role, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> users = userMapper.findAll(keyword, role, status);
        PageInfo<User> pageInfo = new PageInfo<>(users);
        List<AdminUserVO> voList = users.stream().map(this::toAdminUserVO).collect(Collectors.toList());
        return PageResult.of(pageInfo, voList);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        userMapper.updateStatus(userId, status);
    }

    @Override
    public void updateUserRole(Long userId, String role) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        userMapper.updateRole(userId, role);
    }

    private AdminUserVO toAdminUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
