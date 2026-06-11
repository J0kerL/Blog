package com.blog.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.PostCreateDTO;
import com.blog.dto.PostStatusDTO;
import com.blog.service.PostService;
import com.blog.vo.PostListVO;
import com.blog.vo.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin-文章管理")
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@SaCheckRole("ROLE_ADMIN")
public class AdminPostController {

    private final PostService postService;

    @Operation(summary = "创建文章")
    @PostMapping
    public Result<PostVO> create(@Valid @RequestBody PostCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(postService.create(userId, dto));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    public Result<PostVO> update(@PathVariable Long id, @Valid @RequestBody PostCreateDTO dto) {
        return Result.ok(postService.update(id, dto));
    }

    @Operation(summary = "更新文章状态")
    @PatchMapping("/{id}/status")
    public Result<PostVO> updateStatus(@PathVariable Long id, @Valid @RequestBody PostStatusDTO dto) {
        return Result.ok(postService.updateStatus(id, dto.getStatus()));
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "文章详情")
    @GetMapping("/{id}")
    public Result<PostVO> getById(@PathVariable Long id) {
        return Result.ok(postService.getById(id));
    }

    @Operation(summary = "Admin 文章列表（含全部状态）")
    @GetMapping
    public Result<PageResult<PostListVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryId) {
        return Result.ok(postService.listAdmin(pageNum, pageSize, keyword, status, categoryId));
    }
}
