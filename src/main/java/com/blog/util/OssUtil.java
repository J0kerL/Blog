package com.blog.util;

import com.aliyun.oss.OSS;
import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传工具类
 *
 * <p>封装阿里云 OSS 的文件上传操作，提供统一的文件上传接口。</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>自动按日期组织文件路径（yyyy/MM/dd）</li>
 *   <li>使用 UUID 生成唯一文件名，避免重名冲突</li>
 *   <li>保留原始文件扩展名</li>
 *   <li>上传失败时抛出业务异常</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Autowired
 * private OssUtil ossUtil;
 *
 * String url = ossUtil.upload(multipartFile);
 * // 结果: https://b1og.oss-cn-beijing.aliyuncs.com/blog/2024/01/15/uuid.jpg
 * }</pre>
 *
 * @author Diamond
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtil {

    /** 阿里云 OSS 客户端 */
    private final OSS ossClient;
    
    /** OSS 配置信息 */
    private final OssConfig ossConfig;

    /**
     * 上传文件到阿里云 OSS
     *
     * <p>文件存储路径格式：blog/{yyyy/MM/dd}/{uuid}.{extension}</p>
     *
     * @param file 待上传的文件，不能为 null
     * @return 上传成功后的文件访问 URL
     * @throws BusinessException 当文件上传失败时抛出 {@link ResultCode#UPLOAD_FAILED}
     */
    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectKey = "blog/" + datePath + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossConfig.getBucketName(), objectKey, inputStream);
            return ossConfig.getUrlPrefix() + "/" + objectKey;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCode.UPLOAD_FAILED);
        }
    }
}
