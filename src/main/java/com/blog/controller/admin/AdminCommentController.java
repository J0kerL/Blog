package com.blog.controller.admin;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import cn.dev33.satoken.annotation.SaCheckRole;

@Tag(name = "Admin-评论管理")
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@SaCheckRole("ROLE_ADMIN")
public class AdminCommentController {

    private final CommentService commentService;

    @Operation(summary = "Admin 评论列表")
    @GetMapping
    public Result<PageResult<CommentVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(commentService.listAdmin(pageNum, pageSize, postId, status));
    }

    @Operation(summary = "审核通过")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        commentService.approve(id);
        return Result.ok();
    }

    @Operation(summary = "审核拒绝")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        commentService.reject(id);
        return Result.ok();
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }
}
