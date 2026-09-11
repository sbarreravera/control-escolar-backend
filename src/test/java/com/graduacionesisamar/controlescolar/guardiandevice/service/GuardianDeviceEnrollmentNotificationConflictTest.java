package com.graduacionesisamar.controlescolar.guardiandevice.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianDeviceEnrollmentNotificationConflictTest {

    @Mock
    private GuardianDeviceRepository guardianDeviceRepository;

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    @Mock
    private GuardianSessionService guardianSessionService;

    @InjectMocks
    private GuardianDeviceService guardianDeviceService;

    @Test
    void enrollmentContinuesWithoutNotificationsWhenTokenBelongsToAnotherGuardian() {
        Guardian requestedGuardian = guardian(1L);
        Guardian currentGuardian = guardian(2L);

        GuardianDevice existingDevice = new GuardianDevice();
        existingDevice.setId(10L);
        existingDevice.setGuardian(currentGuardian);
        existingDevice.setFcmToken("shared-browser-token");
        existingDevice.setActive(true);

        when(guardianDeviceRepository.findByFcmToken("shared-browser-token"))
                .thenReturn(Optional.of(existingDevice));

        var response = guardianDeviceService.registerFromEnrollment(
                requestedGuardian,
                new RegisterGuardianDeviceRequest(
                        "shared-browser-token",
                        "Navegador de prueba"
                )
        );

        assertNull(response);
        verify(guardianDeviceRepository, never())
                .save(any(GuardianDevice.class));
    }

    private Guardian guardian(Long id) {
        School school = new School();
        school.setId(10L);
        school.setCode("ESC-TEST");
        school.setName("Escuela de Prueba");
        school.setActive(true);

        Guardian guardian = new Guardian();
        guardian.setId(id);
        guardian.setSchool(school);
        guardian.setFullName("Tutor de Prueba " + id);
        guardian.setActive(true);
        return guardian;
    }
}
