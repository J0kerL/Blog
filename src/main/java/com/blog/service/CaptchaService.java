package com.blog.service;

import com.blog.vo.CaptchaVO;
import com.blog.vo.SliderCaptchaVO;

public interface CaptchaService {

    /** 生成图形验证码，返回 key + Base64 图片 */
    CaptchaVO generateCaptcha();

    /** 校验验证码（一次性，验证后自动删除） */
    void verifyCaptcha(String captchaKey, String captchaCode);

    /** 生成滑块验证码挑战 */
    SliderCaptchaVO generateSliderCaptcha();

    /** 校验滑块拖动结果，成功后返回一次性注册 token */
    SliderCaptchaVO verifySliderCaptcha(String captchaKey, Integer sliderX);

    /** 消费滑块验证码成功 token，一次性使用 */
    void verifySliderCaptchaToken(String captchaKey, String captchaToken);
}
