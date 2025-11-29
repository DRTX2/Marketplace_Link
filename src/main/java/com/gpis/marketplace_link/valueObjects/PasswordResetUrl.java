package com.gpis.marketplace_link.valueObjects;

import lombok.Getter;

@Getter
public class PasswordResetUrl {

    private final String url;

    public PasswordResetUrl(String frontendBaseUrl, String token) {
        this.url = frontendBaseUrl + "?token=" + token;
    }

    @Override
    public String toString() {
        return url;
    }
}
