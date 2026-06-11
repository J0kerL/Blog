package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.CategoryDTO;
import com.blog.service.CategoryService;
import com.blog.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import cn.dev33.satoken.annotation.SaCheckRole;
import java.util.List;

@Tag(name = "Admin-分类管理")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@SaCheckRole("ROLE_ADMIN")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.listAll());
    }

    @Operation(summary = "搜索分类")
    @GetMapping("/search")
    public Result<List<CategoryVO>> search(@RequestParam(required = false) String keyword) {
        return Result.ok(categoryService.search(keyword));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CategoryDTO dto) {
        return Result.ok(categoryService.create(dto));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        return Result.ok(categoryService.update(id, dto));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
