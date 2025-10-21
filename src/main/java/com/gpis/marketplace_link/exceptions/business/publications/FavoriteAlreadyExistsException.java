package com.gpis.marketplace_link.exceptions.business.publications;

import com.gpis.marketplace_link.exceptions.business.BusinessException;

public class FavoriteAlreadyExistsException extends BusinessException {
    public FavoriteAlreadyExistsException(String message) {
        super(message);
    }
}

