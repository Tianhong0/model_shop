package org.majun.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.common.ResultCode;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.enums.EmailCodeScene;
import org.majun.backend.service.EmailCodeService;
import org.majun.backend.util.RedisUtil;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final long CODE_EXPIRE_SECONDS = 300;
    private static final long SEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ERROR_TIMES = 5;

    private final RedisUtil redisUtil;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public void sendCode(EmailCodeScene scene, String email) {
        String normalizedEmail = normalizeEmail(email);
        String cooldownKey = buildCooldownKey(scene, normalizedEmail);
        if (redisUtil.hasKey(cooldownKey)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        redisUtil.setString(buildCodeKey(scene, normalizedEmail), code, CODE_EXPIRE_SECONDS);
        redisUtil.deleteKey(buildErrorKey(scene, normalizedEmail));
        redisUtil.setString(cooldownKey, "1", SEND_COOLDOWN_SECONDS);

        try {
            sendMail(normalizedEmail, code);
            log.info("邮箱验证码发送成功, scene={}, email={}", scene.getCode(), normalizedEmail);
        } catch (Exception e) {
            redisUtil.deleteKey(buildCodeKey(scene, normalizedEmail));
            redisUtil.deleteKey(cooldownKey);
            log.error("邮箱验证码发送失败, scene={}, email={}", scene.getCode(), normalizedEmail, e);
            throw new BusinessException("验证码发送失败，请稍后重试");
        }
    }

    @Override
    public void verifyCode(EmailCodeScene scene, String email, String code, boolean consume) {
        String normalizedEmail = normalizeEmail(email);
        String codeKey = buildCodeKey(scene, normalizedEmail);
        String expectedCode = redisUtil.getString(codeKey);
        if (!StringUtils.hasText(expectedCode)) {
            throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
        }

        if (!expectedCode.equals(String.valueOf(code).trim())) {
            String errorKey = buildErrorKey(scene, normalizedEmail);
            long errors = redisUtil.increment(errorKey, 1);
            if (errors == 1) {
                redisUtil.expire(errorKey, CODE_EXPIRE_SECONDS);
            }
            if (errors >= MAX_ERROR_TIMES) {
                redisUtil.deleteKey(codeKey);
                redisUtil.deleteKey(errorKey);
                throw new BusinessException("验证码错误次数过多，请重新获取");
            }
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }

        if (consume) {
            redisUtil.deleteKey(codeKey);
            redisUtil.deleteKey(buildErrorKey(scene, normalizedEmail));
        }
    }

    private void sendMail(String to, String code) {
        if (!StringUtils.hasText(mailFrom)) {
            throw new IllegalStateException("未配置发件邮箱");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject("3DShop 邮箱验证码");
        message.setText("您的验证码为：" + code + "，5分钟内有效。若非本人操作，请忽略此邮件。");
        mailSender.send(message);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        return email.trim().toLowerCase();
    }

    private String buildCodeKey(EmailCodeScene scene, String email) {
        return "email:code:" + scene.getCode() + ":" + email;
    }

    private String buildCooldownKey(EmailCodeScene scene, String email) {
        return "email:code:cooldown:" + scene.getCode() + ":" + email;
    }

    private String buildErrorKey(EmailCodeScene scene, String email) {
        return "email:code:error:" + scene.getCode() + ":" + email;
    }
}
