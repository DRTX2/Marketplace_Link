package com.gpis.marketplace_link.exceptions.business.users;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserBlockedException extends RuntimeException {

    private final LocalDateTime blockedUntil;

    public UserBlockedException(String message, LocalDateTime blockedUntil) {
        super(message);
        this.blockedUntil = blockedUntil;
    }

}
