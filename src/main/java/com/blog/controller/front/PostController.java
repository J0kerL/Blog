package com.blog.controller.front;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.service.CommentService;
import com.blog.service.PostService;
import com.blog.vo.CommentVO;
import com.blog.vo.PostListVO;
import com.blog.vo.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台文章接口", description = "文章浏览")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    @Operation(summary = "文章列表（分页、搜索、分类/标签过滤）")
    @GetMapping
    public Result<PageResult<PostListVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId) {
        return Result.ok(postService.listPublished(pageNum, pageSize, keyword, categoryId, tagId));
    }

    @Operation(summary = "文章详情（通过 ID）")
    @GetMapping("/{id}")
    public Result<PostVO> getById(@PathVariable Long id) {
        return Result.ok(postService.getByIdForView(id));
    }

    @Operation(summary = "文章详情（通过 Slug）")
    @GetMapping("/slug/{slug}")
    public Result<PostVO> getBySlug(@PathVariable String slug) {
        return Result.ok(postService.getBySlug(slug));
    }

    @Operation(summary = "文章评论列表")
    @GetMapping("/{postId}/comments")
    public Result<List<CommentVO>> listComments(@PathVariable Long postId) {
        return Result.ok(commentService.listByPostId(postId));
    }
}
