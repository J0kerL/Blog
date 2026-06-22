package com.blog.controller.front;

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

@Tag(name = "用户文章管理", description = "普通用户的文章 CRUD 操作")
@RestController
@RequestMapping("/api/user/posts")
@RequiredArgsConstructor
public class UserPostController {

    private final PostService postService;

    @Operation(summary = "创建文章")
    @PostMapping
    public Result<PostVO> create(@Valid @RequestBody PostCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(postService.createForUser(userId, dto));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    public Result<PostVO> update(@PathVariable Long id, @Valid @RequestBody PostCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(postService.updateForUser(userId, id, dto));
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        postService.deleteForUser(userId, id);
        return Result.ok();
    }

    @Operation(summary = "更新文章状态")
    @PatchMapping("/{id}/status")
    public Result<PostVO> updateStatus(@PathVariable Long id, @Valid @RequestBody PostStatusDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(postService.updateStatusForUser(userId, id, dto.getStatus()));
    }

    @Operation(summary = "获取文章详情")
    @GetMapping("/{id}")
    public Result<PostVO> getById(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(postService.getByIdForUser(userId, id));
    }

    @Operation(summary = "我的文章列表")
    @GetMapping
    public Result<PageResult<PostListVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(postService.listByUser(userId, pageNum, pageSize, keyword, status));
    }
}
