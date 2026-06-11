package com.blog.service;

import com.blog.vo.CaptchaVO;

public interface CaptchaService {

    /** 生成图形验证码，返回 key + Base64 图片 */
    CaptchaVO generateCaptcha();

    /** 校验验证码（一次性，验证后自动删除） */
    void verifyCaptcha(String captchaKey, String captchaCode);
}
