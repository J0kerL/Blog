package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cn.dev33.satoken.annotation.SaCheckRole;

@Tag(name = "Admin-文件上传")
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
@SaCheckRole("ROLE_ADMIN")
public class AdminFileController {

    private final FileService fileService;

    @Operation(summary = "上传图片到阿里云 OSS")
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }
}
