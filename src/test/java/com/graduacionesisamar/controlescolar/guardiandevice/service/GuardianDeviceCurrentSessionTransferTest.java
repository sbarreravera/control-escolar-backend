package com.graduacionesisamar.controlescolar.guardiandevice.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianDeviceCurrentSessionTransferTest {

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
    void currentSessionTransfersTokenOwnedByAnotherGuardian() {
        School school = new School();
        school.setId(4L);
        school.setName("ESCUELA DE PRUEBA");

        Guardian requestedGuardian = new Guardian();
        requestedGuardian.setId(1L);
        requestedGuardian.setSchool(school);
        requestedGuardian.setActive(true);

        Guardian previousGuardian = new Guardian();
        previousGuardian.setId(2L);
        previousGuardian.setSchool(school);
        previousGuardian.setActive(true);

        GuardianDevice existingDevice = new GuardianDevice();
        existingDevice.setId(10L);
        existingDevice.setGuardian(previousGuardian);
        existingDevice.setFcmToken("token-1");
        existingDevice.setDeviceName("Dispositivo anterior");
        existingDevice.setActive(true);
        existingDevice.setRegisteredAt(
                OffsetDateTime.parse("2026-09-11T10:00:00-06:00")
        );

        GuardianPrincipal principal = new GuardianPrincipal(
                100L,
                1L,
                null,
                4L,
                "Tutor de prueba",
                "ESCUELA DE PRUEBA",
                OffsetDateTime.now().plusDays(30)
        );

        RegisterGuardianDeviceRequest request =
                new RegisterGuardianDeviceRequest(
                        "token-1",
                        "Dispositivo Apple"
                );

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(requestedGuardian));
        when(guardianDeviceRepository.findByFcmToken("token-1"))
                .thenReturn(Optional.of(existingDevice));
        when(guardianDeviceRepository.save(existingDevice))
                .thenReturn(existingDevice);

        GuardianDeviceResponse response =
                guardianDeviceService.registerForCurrentSession(
                        principal,
                        request
                );

        assertSame(requestedGuardian, existingDevice.getGuardian());
        assertEquals("Dispositivo Apple", existingDevice.getDeviceName());
        assertTrue(existingDevice.isActive());
        assertEquals(1L, response.guardianId());
        assertEquals(10L, response.id());

        verify(guardianSessionService).revokeForDevice(10L);
        verify(guardianSessionService).attachDevice(100L, 1L, 10L);
        verify(guardianDeviceRepository).save(existingDevice);
    }
}
