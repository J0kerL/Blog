package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.TagDTO;
import com.blog.service.TagService;
import com.blog.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import cn.dev33.satoken.annotation.SaCheckRole;
import java.util.List;

@Tag(name = "Admin-标签管理")
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
@SaCheckRole("ROLE_ADMIN")
public class AdminTagController {

    private final TagService tagService;

    @Operation(summary = "标签列表")
    @GetMapping
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.listAll());
    }

    @Operation(summary = "搜索标签")
    @GetMapping("/search")
    public Result<List<TagVO>> search(@RequestParam(required = false) String keyword) {
        return Result.ok(tagService.search(keyword));
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public Result<TagVO> create(@Valid @RequestBody TagDTO dto) {
        return Result.ok(tagService.create(dto));
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<TagVO> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        return Result.ok(tagService.update(id, dto));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
