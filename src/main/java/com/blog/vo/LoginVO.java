package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "Sa-Token 令牌")
    private String token;

    @Schema(description = "令牌前缀")
    private String tokenPrefix;

    @Schema(description = "用户信息")
    private UserVO user;
}
