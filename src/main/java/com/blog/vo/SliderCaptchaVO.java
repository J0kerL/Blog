package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "滑块验证码响应")
public class SliderCaptchaVO {

    @Schema(description = "滑块验证码 key，提交校验和注册时携带", example = "a1b2c3d4")
    private String captchaKey;

    @Schema(description = "滑块轨道宽度", example = "320")
    private Integer trackWidth;

    @Schema(description = "滑块宽度", example = "46")
    private Integer sliderWidth;

    @Schema(description = "过期时间，单位秒", example = "300")
    private Integer expiresInSeconds;

    @Schema(description = "滑块校验成功后返回的一次性 token，注册时作为 captchaCode 提交")
    private String captchaToken;
}
