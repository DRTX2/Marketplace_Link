package com.gpis.marketplace_link.exceptions.business.users;

public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException() { super("Tu cuenta está bloqueada. Contacta soporte."); }
}