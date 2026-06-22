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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final CaptchaService captchaService;
    private final OssUtil ossUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final Set<String> AVATAR_ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final int MAX_PAGE_SIZE = 100;
    
    /** 用户信息缓存 Key 前缀 */
    private static final String CACHE_KEY_PREFIX = "cache:user:profile:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    @Override
    public LoginVO register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 校验验证码
        captchaService.verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
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

        if (!PASSWORD_ENCODER.matches(dto.getPassword(), user.getPassword())) {
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
    @Transactional(readOnly = true)
    public UserVO getProfile(Long userId) {
        // 尝试从缓存获取
        String cacheKey = CACHE_KEY_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, UserVO.class);
            } catch (JsonProcessingException e) {
                log.warn("反序列化用户缓存失败: userId={}", userId, e);
            }
        }
        
        // 从数据库查询
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        UserVO result = toUserVO(user);
        
        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("序列化用户缓存失败: userId={}", userId, e);
        }
        
        return result;
    }

    @Override
    @Transactional
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
        
        // 清除用户缓存
        clearUserCache(userId);
        
        return getProfile(userId);
    }

    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (userMapper.findById(userId) == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 校验文件类型：Content-Type + 扩展名白名单
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lowerName = originalFilename.toLowerCase();
            boolean allowed = AVATAR_ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
            if (!allowed) {
                throw new BusinessException("仅支持 jpg/jpeg/png/gif/webp 格式的图片");
            }
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像图片大小不能超过 5MB");
        }
        // 上传到 OSS
        String avatarUrl = ossUtil.upload(file);
        // 更新数据库
        User updateParam = new User();
        updateParam.setId(userId);
        updateParam.setAvatar(avatarUrl);
        userMapper.updateProfileSelective(updateParam);
        
        // 清除用户缓存
        clearUserCache(userId);
        
        return avatarUrl;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 校验旧密码
        if (!PASSWORD_ENCODER.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        // 更新新密码
        user.setPassword(PASSWORD_ENCODER.encode(dto.getNewPassword()));
        userMapper.updatePassword(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordDTO dto) {
        // 先验证邮箱是否存在
        User user = userMapper.findByEmail(dto.getEmail());
        if (user == null) {
            throw new BusinessException(ResultCode.EMAIL_NOT_FOUND);
        }
        // 业务校验通过后，最后校验验证码（一次性消耗）
        captchaService.verifyCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        // 更新密码
        user.setPassword(PASSWORD_ENCODER.encode(dto.getNewPassword()));
        userMapper.updatePassword(user);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    // ========== Admin ==========

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminUserVO> listUsers(int pageNum, int pageSize, String keyword, String role, Integer status) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<User> users = userMapper.findAll(keyword, role, status);
        PageInfo<User> pageInfo = new PageInfo<>(users);
        List<AdminUserVO> voList = users.stream().map(this::toAdminUserVO).collect(Collectors.toList());
        return PageResult.of(pageInfo, voList);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        userMapper.updateStatus(userId, status);
    }

    @Override
    @Transactional
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
    
    /**
     * 清除用户缓存
     */
    private void clearUserCache(Long userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        log.debug("用户缓存已清除: userId={}", userId);
    }
}
