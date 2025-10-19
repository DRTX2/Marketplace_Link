package com.gpis.marketplace_link.exceptions.business.incidences;

public class IncidenceNotBelongToModeratorException extends RuntimeException {

    public IncidenceNotBelongToModeratorException(String message) {
        super(message);
    }

}
