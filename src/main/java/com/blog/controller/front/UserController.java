package com.blog.controller.front;

import cn.dev33.satoken.stp.StpUtil;
import com.blog.common.Result;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.service.UserService;
import com.blog.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户接口", description = "个人信息管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<UserVO> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.getProfile(userId));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.updateProfile(userId, dto));
    }

    @Operation(summary = "上传头像", description = "上传图片到 OSS 并更新用户头像 URL")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.uploadAvatar(userId, file));
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.changePassword(userId, dto);
        return Result.ok();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }
}
