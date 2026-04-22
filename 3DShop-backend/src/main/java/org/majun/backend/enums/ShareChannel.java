package org.majun.backend.enums;

/**
 * 分享渠道枚举
 */
public enum ShareChannel {

    WECHAT("WECHAT", "微信好友"),
    MOMENTS("MOMENTS", "朋友圈"),
    QQ("QQ", "QQ"),
    WEIBO("WEIBO", "微博"),
    COPY_LINK("COPY_LINK", "复制链接");

    private final String code;
    private final String description;

    ShareChannel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ShareChannel fromCode(String code) {
        for (ShareChannel channel : values()) {
            if (channel.code.equals(code)) {
                return channel;
            }
        }
        return null;
    }
}
