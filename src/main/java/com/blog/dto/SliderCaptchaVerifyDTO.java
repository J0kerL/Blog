package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "滑块验证码校验请求")
public class SliderCaptchaVerifyDTO {

    @NotBlank(message = "验证码 key 不能为空")
    @Schema(description = "滑块验证码 key", example = "a1b2c3d4")
    private String captchaKey;

    @NotNull(message = "滑块位置不能为空")
    @Schema(description = "滑块最终横向位置", example = "274")
    private Integer sliderX;
}
