package com.blog.controller.front;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.blog.common.Result;
import com.blog.dto.CommentCreateDTO;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "前台评论接口")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "提交评论（已登录用户自动关联，未登录需填昵称）")
    @PostMapping
    public Result<CommentVO> create(@Valid @RequestBody CommentCreateDTO dto) {
        Long userId = null;
        try {
            userId = StpUtil.getLoginIdAsLong();
        } catch (NotLoginException ignored) {
            // 未登录用户也可以评论
        }
        return Result.ok(commentService.create(userId, dto));
    }
}
