package com.gpis.marketplace_link.exceptions.business.publications;

import java.io.Serial;

public class UploadFolderException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    public UploadFolderException(String message) {
        super(message);
    }

    public UploadFolderException(String message, Throwable cause) {
        super(message, cause);
    }
}
