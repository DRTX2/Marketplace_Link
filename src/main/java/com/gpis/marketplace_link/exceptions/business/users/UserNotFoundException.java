package com.gpis.marketplace_link.exceptions.business.users;

import com.gpis.marketplace_link.exceptions.business.BusinessException;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

