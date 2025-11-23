package com.gpis.marketplace_link.jobs;

import com.gpis.marketplace_link.services.incidence.IncidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class IncidenceAutocloser {

    private final IncidenceService incidenceService;

    // segundos, minutos, horas, dias, mes, dia_semana
    @Scheduled(cron = "0 0 3 * * *")
    public void closeInactiveIncidences() {
        incidenceService.autoclose();
    }
}
