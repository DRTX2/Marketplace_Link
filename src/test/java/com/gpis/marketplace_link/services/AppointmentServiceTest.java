package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.application.services.AppointmentServiceImpl;
import com.gpis.marketplace_link.entities.Appointment;
import com.gpis.marketplace_link.domain.valueObjets.AppointmentStatus;
import com.gpis.marketplace_link.infrastructure.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Test
    void shouldDetectOverlappingAppointments1() {

        LocalDateTime start1 = LocalDateTime.of(2025,9,10,10,0);
        LocalDateTime end1 = LocalDateTime.of(2025,9,10,11,0);
        Appointment appointment = new Appointment(null, start1, end1, true, Instant.now(), null, AppointmentStatus.SCHEDULED, null, null, null);

        LocalDateTime fromUserStart = LocalDateTime.of(2025, 9, 10, 10, 30);
        LocalDateTime fromUserEnd = LocalDateTime.of(2025, 9, 10, 11, 0);

        // Mock con 4 parámetros: id, status, start, end
        when(appointmentRepository.findOverlappingAppointmentsForPatient(anyLong(), any(AppointmentStatus.class), any(), any()))
                .thenReturn(List.of(appointment));
        when(appointmentRepository.findOverlappingAppointmentsForDoctor(anyLong(), any(AppointmentStatus.class), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.findOverlappingAppointmentsForConsultory(anyLong(), any(AppointmentStatus.class), any(), any()))
                .thenReturn(List.of());

        boolean result = appointmentService.hasOverlappingAppointment(1L, 1L, 1L, fromUserStart, fromUserEnd);
        assertTrue(result);

        verify(appointmentRepository).findOverlappingAppointmentsForPatient(anyLong(), any(AppointmentStatus.class), any(), any());
        verify(appointmentRepository).findOverlappingAppointmentsForDoctor(anyLong(), any(AppointmentStatus.class), any(), any());
        verify(appointmentRepository).findOverlappingAppointmentsForConsultory(anyLong(), any(AppointmentStatus.class), any(), any());
    }

    @Test
    void shouldDetectOverlappingAppointments2() {

        LocalDateTime fromUserStart = LocalDateTime.of(2025, 9, 10, 19, 0);
        LocalDateTime fromUserEnd = LocalDateTime.of(2025, 9, 10, 20, 0);

        // Mock con 4 parámetros: id, status, start, end
        when(appointmentRepository.findOverlappingAppointmentsForPatient(anyLong(), any(AppointmentStatus.class), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.findOverlappingAppointmentsForDoctor(anyLong(), any(AppointmentStatus.class), any(), any()))
                .thenReturn(List.of());
        when(appointmentRepository.findOverlappingAppointmentsForConsultory(anyLong(), any(AppointmentStatus.class), any(), any()))
                .thenReturn(List.of());

        boolean result = appointmentService.hasOverlappingAppointment(1L, 1L, 1L, fromUserStart, fromUserEnd);
        assertFalse(result);

        verify(appointmentRepository).findOverlappingAppointmentsForPatient(anyLong(), any(AppointmentStatus.class), any(), any());
        verify(appointmentRepository).findOverlappingAppointmentsForDoctor(anyLong(), any(AppointmentStatus.class), any(), any());
        verify(appointmentRepository).findOverlappingAppointmentsForConsultory(anyLong(), any(AppointmentStatus.class), any(), any());
    }
}
