package com.gpis.marketplace_link.exceptions.business.users;

public class AccountPendingVerificationException extends RuntimeException {
    public AccountPendingVerificationException() { super("Tu cuenta está pendiente de verificación."); }
}
