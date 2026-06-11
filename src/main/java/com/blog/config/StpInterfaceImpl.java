package com.blog.config;

import cn.dev33.satoken.stp.StpInterface;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 自定义权限验证实现
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        Long userId = Long.parseLong(loginId.toString());
        User user = userMapper.findById(userId);
        if (user != null && user.getRole() != null) {
            roles.add(user.getRole());
        }
        return roles;
    }
}
