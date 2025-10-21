package com.gpis.marketplace_link.exceptions.business.publications;

import com.gpis.marketplace_link.exceptions.business.BusinessException;

public class FavoriteNotFoundException extends BusinessException {
    public FavoriteNotFoundException(String message) {
        super(message);
    }
}

