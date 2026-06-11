package com.blog.config;

import cn.dev33.satoken.stp.StpInterface;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token 自定义权限验证实现（角色查询带 Redis 缓存）
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ROLE_CACHE_PREFIX = "role:";

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        String cacheKey = ROLE_CACHE_PREFIX + userId;

        // 先查 Redis 缓存
        String cachedRole = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedRole != null) {
            if (cachedRole.isEmpty()) {
                return new ArrayList<>();
            }
            return new ArrayList<>(Collections.singletonList(cachedRole));
        }

        // 缓存未命中，查询数据库
        List<String> roles = new ArrayList<>();
        User user = userMapper.findById(userId);
        if (user != null && user.getRole() != null) {
            roles.add(user.getRole());
            // 缓存角色，10 分钟过期
            stringRedisTemplate.opsForValue().set(cacheKey, user.getRole(), 10, TimeUnit.MINUTES);
        } else {
            // 缓存空值，防止穿透，较短过期时间
            stringRedisTemplate.opsForValue().set(cacheKey, "", 2, TimeUnit.MINUTES);
        }
        return roles;
    }
}
