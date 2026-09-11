package com.graduacionesisamar.controlescolar.guardiandevice.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
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

    private static final String DEVICE_TOKEN_CONFLICT_MESSAGE =
            "Device token is already registered to another guardian";

    private final GuardianDeviceRepository guardianDeviceRepository;
    private final GuardianRepository guardianRepository;
    private final SchoolAccessService schoolAccessService;
    private final GuardianSessionService guardianSessionService;

    public GuardianDeviceResponse register(
            Long guardianId,
            RegisterGuardianDeviceRequest request
    ) {
        Guardian guardian = findGuardian(guardianId);
        requireGuardianSchoolAccess(guardian);

        return registerDevice(guardian, request, false);
    }

    /**
     * Registers a device after its temporary enrollment
     * invitation has been validated. Notification registration is optional,
     * so a token already owned by another guardian must not block portal access.
     */
    public GuardianDeviceResponse registerFromEnrollment(
            Guardian guardian,
            RegisterGuardianDeviceRequest request
    ) {
        try {
            return registerDevice(guardian, request, false);
        } catch (ResponseStatusException exception) {
            if (HttpStatus.CONFLICT.equals(exception.getStatusCode())
                    && DEVICE_TOKEN_CONFLICT_MESSAGE.equals(
                    exception.getReason()
            )) {
                return null;
            }
            throw exception;
        }
    }

    /**
     * Registers notifications from an already authenticated guardian session.
     * In this flow possession of the current FCM token proves that the browser
     * is the device being configured, so an old ownership can be transferred
     * safely after revoking sessions that referenced that device record.
     */
    public GuardianDeviceResponse registerForCurrentSession(
            GuardianPrincipal principal,
            RegisterGuardianDeviceRequest request
    ) {
        Guardian guardian = findGuardian(principal.guardianId());
        if (!guardian.getSchool().getId().equals(principal.schoolId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Guardian not found"
            );
        }

        GuardianDeviceResponse device = registerDevice(
                guardian,
                request,
                true
        );
        guardianSessionService.attachDevice(
                principal.sessionId(),
                principal.guardianId(),
                device.id()
        );
        return device;
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
        guardianSessionService.revokeForDevice(deviceId);

        return toResponse(
                guardianDeviceRepository.save(device)
        );
    }

    public int deactivateAll(Long guardianId) {
        List<GuardianDevice> devices = guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        guardianId
                );
        devices.forEach(device -> device.setActive(false));
        guardianDeviceRepository.saveAll(devices);
        return devices.size();
    }

    private GuardianDeviceResponse registerDevice(
            Guardian guardian,
            RegisterGuardianDeviceRequest request,
            boolean allowOwnershipTransfer
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
                        request,
                        allowOwnershipTransfer
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
            RegisterGuardianDeviceRequest request,
            boolean allowOwnershipTransfer
    ) {
        boolean belongsToAnotherGuardian = !Objects.equals(
                device.getGuardian().getId(),
                guardian.getId()
        );

        if (belongsToAnotherGuardian && !allowOwnershipTransfer) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    DEVICE_TOKEN_CONFLICT_MESSAGE
            );
        }

        if (belongsToAnotherGuardian) {
            guardianSessionService.revokeForDevice(device.getId());
            device.setGuardian(guardian);
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
