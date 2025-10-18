package com.gpis.marketplace_link.exceptions.business.publications;

import java.io.Serial;


public class DangerousDictionaryLoadException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DangerousDictionaryLoadException(String message) {
        super(message);
    }

    public DangerousDictionaryLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
