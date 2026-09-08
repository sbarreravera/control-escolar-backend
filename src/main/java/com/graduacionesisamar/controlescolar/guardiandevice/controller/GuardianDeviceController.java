package com.graduacionesisamar.controlescolar.guardiandevice.controller;

import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes REST operations for guardian device management.
 */
@RestController
@RequestMapping("/api/v1/guardians/{guardianId}/devices")
@RequiredArgsConstructor
public class GuardianDeviceController {

    private final GuardianDeviceService guardianDeviceService;

    /**
     * Registers a device for a guardian.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianDeviceResponse register(
            @PathVariable Long guardianId,
            @Valid @RequestBody
            RegisterGuardianDeviceRequest request
    ) {
        return guardianDeviceService.register(
                guardianId,
                request
        );
    }

    /**
     * Returns the active devices registered for a guardian.
     */
    @GetMapping
    public List<GuardianDeviceResponse> findActiveByGuardian(
            @PathVariable Long guardianId
    ) {
        return guardianDeviceService.findActiveByGuardian(
                guardianId
        );
    }

    /**
     * Deactivates a guardian device.
     */
    @PatchMapping("/{deviceId}/deactivate")
    public GuardianDeviceResponse deactivate(
            @PathVariable Long guardianId,
            @PathVariable Long deviceId
    ) {
        return guardianDeviceService.deactivate(
                guardianId,
                deviceId
        );
    }
}