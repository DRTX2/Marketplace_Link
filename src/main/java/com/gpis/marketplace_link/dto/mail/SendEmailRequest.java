package com.gpis.marketplace_link.dto.mail;

import com.gpis.marketplace_link.valueObjects.EmailType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendEmailRequest {
    @JsonProperty("email_to")
    private String emailTo;

    @JsonProperty("email_type")
    private EmailType emailType;
}
