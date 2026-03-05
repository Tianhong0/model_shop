package org.majun.backend.enums;

public enum EmailCodeScene {
    REGISTER("register"),
    ADMIN_REGISTER("admin-register"),
    CHANGE_EMAIL("change-email"),
    CHANGE_PASSWORD("change-password"),
    FORGOT_PASSWORD("forgot-password");

    private final String code;

    EmailCodeScene(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
