package com.gpis.marketplace_link.jobs;

import com.gpis.marketplace_link.services.publications.PublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PublicationSuspender {

    private final PublicationService service;

    @Value("${SUSPENDED_TIME_DAYS}")
    private Integer suspendedTimeDays;

    @Scheduled(cron="0 0 0 * * *")
    public void suspendedPublications(){

        this.service.suspendedPublicationsOlderThan(suspendedTimeDays);
    }

}
