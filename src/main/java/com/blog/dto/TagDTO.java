package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "标签创建/更新请求")
public class TagDTO {

    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称", example = "Spring Boot")
    private String name;

    @Schema(description = "URL 别名", example = "spring-boot")
    private String slug;
}
