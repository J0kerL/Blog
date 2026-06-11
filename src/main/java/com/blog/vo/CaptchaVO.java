package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "图形验证码响应")
public class CaptchaVO {

    @Schema(description = "验证码 key（提交时带上）", example = "a1b2c3d4")
    private String captchaKey;

    @Schema(description = "验证码图片 Base64", example = "data:image/png;base64,iVBOR...")
    private String captchaImage;
}
