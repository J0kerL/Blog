package com.blog.controller.front;

import com.blog.common.Result;
import com.blog.dto.ForgotPasswordDTO;
import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.dto.SliderCaptchaVerifyDTO;
import com.blog.service.CaptchaService;
import com.blog.service.UserService;
import com.blog.vo.CaptchaVO;
import com.blog.vo.LoginVO;
import com.blog.vo.SliderCaptchaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口", description = "注册、登录、退出、验证码")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CaptchaService captchaService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        return Result.ok(captchaService.generateCaptcha());
    }

    @Operation(summary = "获取滑块验证码")
    @GetMapping("/captcha/slider")
    public Result<SliderCaptchaVO> getSliderCaptcha() {
        return Result.ok(captchaService.generateSliderCaptcha());
    }

    @Operation(summary = "校验滑块验证码")
    @PostMapping("/captcha/slider/verify")
    public Result<SliderCaptchaVO> verifySliderCaptcha(@Valid @RequestBody SliderCaptchaVerifyDTO dto) {
        return Result.ok(captchaService.verifySliderCaptcha(dto.getCaptchaKey(), dto.getSliderX()));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @Operation(summary = "用户登录", description = "需要先调 /api/auth/captcha 获取验证码")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @Operation(summary = "忘记密码", description = "通过邮箱 + 验证码重置密码")
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        userService.forgotPassword(dto);
        return Result.ok();
    }
}
