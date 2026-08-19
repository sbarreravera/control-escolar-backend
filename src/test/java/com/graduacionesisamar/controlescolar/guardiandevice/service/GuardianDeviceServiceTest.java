package com.graduacionesisamar.controlescolar.guardiandevice.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianDeviceServiceTest {

    private static final OffsetDateTime REGISTERED_AT =
            OffsetDateTime.parse("2026-08-19T10:00:00-06:00");

    @Mock
    private GuardianDeviceRepository guardianDeviceRepository;

    @Mock
    private GuardianRepository guardianRepository;

    @InjectMocks
    private GuardianDeviceService guardianDeviceService;

    @Test
    void registerCreatesNewDevice() {
        Guardian guardian = createGuardian(1L, true);

        RegisterGuardianDeviceRequest request =
                new RegisterGuardianDeviceRequest(
                        "  token-1  ",
                        "  Teléfono principal  "
                );

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));

        when(guardianDeviceRepository.findByFcmToken("token-1"))
                .thenReturn(Optional.empty());

        when(guardianDeviceRepository.save(any(GuardianDevice.class)))
                .thenAnswer(invocation -> {
                    GuardianDevice device =
                            invocation.getArgument(0);

                    device.setId(10L);
                    device.setRegisteredAt(REGISTERED_AT);

                    return device;
                });

        GuardianDeviceResponse response =
                guardianDeviceService.register(1L, request);

        ArgumentCaptor<GuardianDevice> captor =
                ArgumentCaptor.forClass(GuardianDevice.class);

        verify(guardianDeviceRepository).save(captor.capture());

        GuardianDevice savedDevice = captor.getValue();

        assertSame(guardian, savedDevice.getGuardian());
        assertEquals("token-1", savedDevice.getFcmToken());
        assertEquals(
                "Teléfono principal",
                savedDevice.getDeviceName()
        );
        assertTrue(savedDevice.isActive());

        assertEquals(10L, response.id());
        assertEquals(1L, response.guardianId());
        assertEquals(
                "Teléfono principal",
                response.deviceName()
        );
        assertTrue(response.active());
        assertEquals(REGISTERED_AT, response.registeredAt());
    }

    @Test
    void registerReactivatesDeviceFromSameGuardian() {
        Guardian guardian = createGuardian(1L, true);

        GuardianDevice existingDevice = createDevice(
                10L,
                guardian,
                "token-1",
                false
        );

        RegisterGuardianDeviceRequest request =
                new RegisterGuardianDeviceRequest(
                        "token-1",
                        "Dispositivo actualizado"
                );

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));

        when(guardianDeviceRepository.findByFcmToken("token-1"))
                .thenReturn(Optional.of(existingDevice));

        when(guardianDeviceRepository.save(existingDevice))
                .thenReturn(existingDevice);

        GuardianDeviceResponse response =
                guardianDeviceService.register(1L, request);

        assertTrue(existingDevice.isActive());
        assertEquals(
                "Dispositivo actualizado",
                existingDevice.getDeviceName()
        );
        assertTrue(response.active());
        assertEquals(10L, response.id());
    }

    @Test
    void registerRejectsTokenFromAnotherGuardian() {
        Guardian requestedGuardian = createGuardian(1L, true);
        Guardian currentGuardian = createGuardian(2L, true);

        GuardianDevice existingDevice = createDevice(
                10L,
                currentGuardian,
                "token-1",
                true
        );

        RegisterGuardianDeviceRequest request =
                new RegisterGuardianDeviceRequest(
                        "token-1",
                        "Dispositivo"
                );

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(requestedGuardian));

        when(guardianDeviceRepository.findByFcmToken("token-1"))
                .thenReturn(Optional.of(existingDevice));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> guardianDeviceService.register(1L, request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );
        assertEquals(
                "Device token is already registered to another guardian",
                exception.getReason()
        );

        verify(guardianDeviceRepository, never())
                .save(any(GuardianDevice.class));
    }

    @Test
    void registerRejectsInactiveGuardian() {
        Guardian guardian = createGuardian(1L, false);

        RegisterGuardianDeviceRequest request =
                new RegisterGuardianDeviceRequest(
                        "token-1",
                        "Dispositivo"
                );

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> guardianDeviceService.register(1L, request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );
        assertEquals(
                "Inactive guardian cannot register devices",
                exception.getReason()
        );

        verifyNoInteractions(guardianDeviceRepository);
    }

    @Test
    void findActiveByGuardianReturnsActiveDevices() {
        Guardian guardian = createGuardian(1L, true);

        GuardianDevice firstDevice = createDevice(
                10L,
                guardian,
                "token-1",
                true
        );

        GuardianDevice secondDevice = createDevice(
                11L,
                guardian,
                "token-2",
                true
        );

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));

        when(guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        1L
                ))
                .thenReturn(List.of(firstDevice, secondDevice));

        List<GuardianDeviceResponse> response =
                guardianDeviceService.findActiveByGuardian(1L);

        assertEquals(2, response.size());
        assertEquals(10L, response.get(0).id());
        assertEquals(11L, response.get(1).id());
        assertTrue(response.get(0).active());
        assertTrue(response.get(1).active());
    }

    @Test
    void deactivateMarksDeviceAsInactive() {
        Guardian guardian = createGuardian(1L, true);

        GuardianDevice device = createDevice(
                10L,
                guardian,
                "token-1",
                true
        );

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));

        when(guardianDeviceRepository
                .findByIdAndGuardian_Id(10L, 1L))
                .thenReturn(Optional.of(device));

        when(guardianDeviceRepository.save(device))
                .thenReturn(device);

        GuardianDeviceResponse response =
                guardianDeviceService.deactivate(1L, 10L);

        assertFalse(device.isActive());
        assertFalse(response.active());

        verify(guardianDeviceRepository).save(device);
    }

    @Test
    void deactivateRejectsUnknownDevice() {
        Guardian guardian = createGuardian(1L, true);

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));

        when(guardianDeviceRepository
                .findByIdAndGuardian_Id(999L, 1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> guardianDeviceService.deactivate(1L, 999L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
        assertEquals(
                "Guardian device not found",
                exception.getReason()
        );

        verify(guardianDeviceRepository, never())
                .save(any(GuardianDevice.class));
    }

    private Guardian createGuardian(
            Long id,
            boolean active
    ) {
        Guardian guardian = new Guardian();
        guardian.setId(id);
        guardian.setActive(active);
        return guardian;
    }

    private GuardianDevice createDevice(
            Long id,
            Guardian guardian,
            String fcmToken,
            boolean active
    ) {
        GuardianDevice device = new GuardianDevice();
        device.setId(id);
        device.setGuardian(guardian);
        device.setFcmToken(fcmToken);
        device.setDeviceName("Dispositivo de prueba");
        device.setActive(active);
        device.setRegisteredAt(REGISTERED_AT);
        return device;
    }
}