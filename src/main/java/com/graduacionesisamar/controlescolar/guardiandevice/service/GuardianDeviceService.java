package com.graduacionesisamar.controlescolar.guardiandevice.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class GuardianDeviceService {

    private final GuardianDeviceRepository guardianDeviceRepository;
    private final GuardianRepository guardianRepository;
    private final SchoolAccessService schoolAccessService;

    public GuardianDeviceResponse register(
            Long guardianId,
            RegisterGuardianDeviceRequest request
    ) {
        Guardian guardian = findGuardian(guardianId);
        requireGuardianSchoolAccess(guardian);

        return registerDevice(guardian, request);
    }

    /**
     * Registers a device after its temporary enrollment
     * invitation has been validated.
     */
    public GuardianDeviceResponse registerFromEnrollment(
            Guardian guardian,
            RegisterGuardianDeviceRequest request
    ) {
        return registerDevice(guardian, request);
    }

    @Transactional(readOnly = true)
    public List<GuardianDeviceResponse> findActiveByGuardian(
            Long guardianId
    ) {
        Guardian guardian = findGuardian(guardianId);
        requireGuardianSchoolAccess(guardian);

        return guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        guardianId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public GuardianDeviceResponse deactivate(
            Long guardianId,
            Long deviceId
    ) {
        Guardian guardian = findGuardian(guardianId);
        requireGuardianSchoolAccess(guardian);

        GuardianDevice device = guardianDeviceRepository
                .findByIdAndGuardian_Id(deviceId, guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian device not found"
                ));

        device.setActive(false);

        return toResponse(
                guardianDeviceRepository.save(device)
        );
    }

    private GuardianDeviceResponse registerDevice(
            Guardian guardian,
            RegisterGuardianDeviceRequest request
    ) {
        if (!Boolean.TRUE.equals(guardian.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inactive guardian cannot register devices"
            );
        }

        String fcmToken = request.fcmToken().trim();

        GuardianDevice device = guardianDeviceRepository
                .findByFcmToken(fcmToken)
                .map(existingDevice -> updateExistingDevice(
                        existingDevice,
                        guardian,
                        request
                ))
                .orElseGet(() -> buildDevice(
                        guardian,
                        fcmToken,
                        request.deviceName()
                ));

        GuardianDevice savedDevice =
                guardianDeviceRepository.save(device);

        return toResponse(savedDevice);
    }

    private GuardianDevice updateExistingDevice(
            GuardianDevice device,
            Guardian guardian,
            RegisterGuardianDeviceRequest request
    ) {
        if (!Objects.equals(
                device.getGuardian().getId(),
                guardian.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Device token is already registered to another guardian"
            );
        }

        device.setDeviceName(trimNullable(request.deviceName()));
        device.setActive(true);

        return device;
    }

    private GuardianDevice buildDevice(
            Guardian guardian,
            String fcmToken,
            String deviceName
    ) {
        GuardianDevice device = new GuardianDevice();
        device.setGuardian(guardian);
        device.setFcmToken(fcmToken);
        device.setDeviceName(trimNullable(deviceName));
        device.setActive(true);
        return device;
    }

    private Guardian findGuardian(Long guardianId) {
        return guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian not found"
                ));
    }

    private void requireGuardianSchoolAccess(Guardian guardian) {
        schoolAccessService.requireAccessToSchool(
                guardian.getSchool().getId()
        );
    }

    private String trimNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

        private GuardianDeviceResponse toResponse(
                GuardianDevice device
        ) {
        return new GuardianDeviceResponse(
                device.getId(),
                device.getGuardian().getId(),
                device.getDeviceName(),
                device.isActive(),
                device.getRegisteredAt(),
                device.getLastUsedAt()
        );
     }
}