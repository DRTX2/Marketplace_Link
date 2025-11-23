package com.gpis.marketplace_link.mail;

import com.gpis.marketplace_link.enums.EmailType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class NotificationMessage {

    private String to;
    private EmailType type;
    private Map<String, String> variables;
    private List<Attachment> attachments;// optional

}
