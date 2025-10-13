package com.gpis.marketplace_link.valueObjects;

//-- VISIBLE,  UNDER_REVIEW , BLOCKED,
public enum PublicationStatus {
    VISIBLE("VISIBLE"),
    UNDER_REVIEW("UNDER_REVIEW"),
    BLOCKED("BLOCKED");

    private final String value;

    PublicationStatus(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
