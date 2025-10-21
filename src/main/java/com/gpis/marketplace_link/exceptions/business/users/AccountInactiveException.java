package com.gpis.marketplace_link.exceptions.business.users;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException() { super("La cuenta no está activa o su estado es inválido."); }
}