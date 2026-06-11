package com.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.service.UserService;
import com.blog.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin-用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@SaCheckRole("ROLE_ADMIN")
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<PageResult<AdminUserVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return Result.ok(userService.listUsers(pageNum, pageSize, keyword, role, status));
    }

    @Operation(summary = "更新用户状态（启用/禁用）")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateUserStatus(id, status);
        return Result.ok();
    }

    @Operation(summary = "更新用户角色")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateUserRole(id, role);
        return Result.ok();
    }
}
