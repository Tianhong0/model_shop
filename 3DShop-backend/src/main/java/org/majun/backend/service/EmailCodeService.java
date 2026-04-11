package org.majun.backend.service;

import org.majun.backend.enums.EmailCodeScene;

/**
 * 邮箱验证码服务接口
 */
public interface EmailCodeService {

    void sendCode(EmailCodeScene scene, String email);

    void verifyCode(EmailCodeScene scene, String email, String code, boolean consume);
}
