package com.graduacionesisamar.controlescolar.guardiandevice.controller;

import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lets an authenticated guardian enable notifications on the current device.
 */
@RestController
@RequestMapping("/api/v1/guardian/devices")
@RequiredArgsConstructor
public class GuardianSelfDeviceController {

    private final GuardianDeviceService guardianDeviceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianDeviceResponse register(
            Authentication authentication,
            @Valid @RequestBody RegisterGuardianDeviceRequest request
    ) {
        return guardianDeviceService.registerForCurrentSession(
                requirePrincipal(authentication),
                request
        );
    }

    private GuardianPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof GuardianPrincipal principal)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Guardian session is required"
            );
        }
        return principal;
    }
}
