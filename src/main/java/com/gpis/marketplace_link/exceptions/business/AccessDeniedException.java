package com.gpis.marketplace_link.exceptions.business;

public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
