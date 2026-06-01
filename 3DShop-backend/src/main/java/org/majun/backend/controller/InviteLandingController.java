package org.majun.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.PromotionConfig;
import org.majun.backend.repository.PromotionConfigRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 邀请落地页控制器 — 处理推广邀请链接的落地展示与注册引导
 */
@Slf4j
@Tag(name = "InviteLanding", description = "邀请链接落地页")
@Controller
@RequiredArgsConstructor
public class InviteLandingController {

    private final PromotionConfigRepository configRepository;

    /** 邀请链接落地页，检测APP并跳转 */
    @GetMapping("/invite/{inviteCode}")
    @ResponseBody
    @Operation(summary = "邀请链接落地页，检测APP并跳转")
    public String inviteLanding(@PathVariable String inviteCode) {
        String scheme = getConfigValue("APP_URL_SCHEME", "threedshop");
        String downloadUrl = getConfigValue("APP_DOWNLOAD_URL", "");

        String schemeUrl = scheme + "://invite?inviteCode=" + inviteCode;

        return buildLandingPage(schemeUrl, downloadUrl, inviteCode, "邀请注册", "注册即可获得积分奖励");
    }

    /** 拼团分享落地页，检测APP并跳转 */
    @GetMapping("/group-buy/{shareCode}")
    @ResponseBody
    @Operation(summary = "拼团分享落地页，检测APP并跳转")
    public String groupBuyLanding(@PathVariable String shareCode) {
        String scheme = getConfigValue("APP_URL_SCHEME", "threedshop");
        String downloadUrl = getConfigValue("APP_DOWNLOAD_URL", "");

        String schemeUrl = scheme + "://group-buy?shareCode=" + shareCode;

        return buildLandingPage(schemeUrl, downloadUrl, shareCode, "拼团邀请", "快来一起拼团吧");
    }

    private String getConfigValue(String key, String defaultValue) {
        PromotionConfig config = configRepository.selectOne(
            new LambdaQueryWrapper<PromotionConfig>()
                .eq(PromotionConfig::getConfigKey, key)
                .eq(PromotionConfig::getStatus, 1)
        );
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    private String buildLandingPage(String schemeUrl, String downloadUrl, String code, String scene, String subtitle) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">\n");
        html.append("<title>3D打印定制商城 - ").append(escapeHtml(scene)).append("</title>\n");
        html.append("<style>\n");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; ");
        html.append("background: linear-gradient(135deg, #00bfff 0%, #0099cc 100%); min-height: 100vh; ");
        html.append("display: flex; align-items: center; justify-content: center; text-align: center; }\n");
        html.append(".card { background: #fff; border-radius: 20px; padding: 48px 32px; margin: 24px; ");
        html.append("box-shadow: 0 20px 60px rgba(0,0,0,0.15); max-width: 360px; width: 100%; }\n");
        html.append(".logo { font-size: 56px; margin-bottom: 16px; }\n");
        html.append(".title { font-size: 22px; font-weight: 700; color: #1a2030; margin-bottom: 8px; }\n");
        html.append(".subtitle { font-size: 14px; color: #94a3b8; margin-bottom: 32px; }\n");
        html.append(".spinner { width: 40px; height: 40px; border: 3px solid #e8f4fd; ");
        html.append("border-top-color: #00bfff; border-radius: 50%; animation: spin 0.8s linear infinite; ");
        html.append("margin: 0 auto 16px; }\n");
        html.append("@keyframes spin { to { transform: rotate(360deg); } }\n");
        html.append(".invite-code { display: inline-block; background: #e8f4fd; color: #0099cc; ");
        html.append("font-size: 28px; font-weight: 700; letter-spacing: 4px; padding: 8px 24px; ");
        html.append("border-radius: 12px; margin-bottom: 16px; }\n");
        html.append(".btn { display: inline-block; background: #00bfff; color: #fff; font-size: 16px; ");
        html.append("font-weight: 600; padding: 12px 36px; border-radius: 999px; text-decoration: none; ");
        html.append("margin-top: 16px; transition: transform 0.2s, box-shadow 0.2s; }\n");
        html.append(".btn:active { transform: scale(0.96); box-shadow: 0 4px 16px rgba(0,191,255,0.4); }\n");
        html.append(".loading-text { font-size: 14px; color: #5a6a7a; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class=\"card\">\n");
        html.append("<div class=\"logo\">&#x1F389;</div>\n");
        html.append("<div class=\"title\">您收到了一个").append(escapeHtml(scene)).append("</div>\n");
        html.append("<div class=\"subtitle\">").append(escapeHtml(subtitle)).append("</div>\n");
        html.append("<div class=\"invite-code\">").append(escapeHtml(code)).append("</div>\n");
        html.append("<div class=\"spinner\" id=\"spinner\"></div>\n");
        html.append("<div class=\"loading-text\" id=\"statusText\">正在打开3D打印定制商城...</div>\n");

        if (StringUtils.hasText(downloadUrl)) {
            html.append("<a class=\"btn\" href=\"").append(escapeHtml(downloadUrl)).append("\">下载APP</a>\n");
        } else {
            html.append("<div class=\"loading-text\" style=\"margin-top:12px;\">如未安装APP，请在应用商店搜索\"3D打印定制商城\"下载</div>\n");
        }

        html.append("</div>\n");
        html.append("<script>\n");
        html.append("(function() {\n");
        html.append("  var schemeUrl = '").append(schemeUrl).append("';\n");
        html.append("  var downloadUrl = '").append(downloadUrl != null ? downloadUrl : "").append("';\n");
        html.append("  var appOpened = false;\n");
        html.append("  var launchTime = Date.now();\n");
        html.append("\n");
        html.append("  function markOpened() { appOpened = true; }\n");
        html.append("  document.addEventListener('visibilitychange', function() {\n");
        html.append("    if (document.hidden) markOpened();\n");
        html.append("  });\n");
        html.append("  window.addEventListener('pagehide', markOpened);\n");
        html.append("  window.addEventListener('blur', markOpened);\n");
        html.append("\n");
        html.append("  var iframe = document.createElement('iframe');\n");
        html.append("  iframe.style.display = 'none';\n");
        html.append("  iframe.src = schemeUrl;\n");
        html.append("  document.body.appendChild(iframe);\n");
        html.append("\n");
        html.append("  setTimeout(function() {\n");
        html.append("    document.getElementById('spinner').style.display = 'none';\n");
        html.append("    if (!appOpened) {\n");
        html.append("      document.getElementById('statusText').innerText = '未检测到APP，请下载安装';\n");
        html.append("      if (downloadUrl) {\n");
        html.append("        window.location.href = downloadUrl;\n");
        html.append("      }\n");
        html.append("    } else {\n");
        html.append("      document.getElementById('statusText').innerText = '已打开APP';\n");
        html.append("    }\n");
        html.append("  }, 2500);\n");
        html.append("\n");
        html.append("  setTimeout(function() {\n");
        html.append("    iframe.parentNode && iframe.parentNode.removeChild(iframe);\n");
        html.append("  }, 5000);\n");
        html.append("})();\n");
        html.append("</script>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
