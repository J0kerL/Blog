package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.service.FileService;
import com.blog.util.OssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final OssUtil ossUtil;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 校验文件类型：Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }

        // 校验文件扩展名白名单
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String lowerName = originalFilename.toLowerCase();
            boolean allowed = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
            if (!allowed) {
                throw new BusinessException("仅支持 jpg/jpeg/png/gif/webp 格式的图片");
            }
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过 10MB");
        }

        return ossUtil.upload(file);
    }
}
