package com.graduacionesisamar.controlescolar.guardianregistration.controller;

import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianRegistrationSettingsResponse;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.UpdateGuardianRegistrationSettingsRequest;
import com.graduacionesisamar.controlescolar.guardianregistration.service.GuardianRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guardian-registration-settings")
@RequiredArgsConstructor
public class GuardianRegistrationSettingsController {

    private final GuardianRegistrationService guardianRegistrationService;

    @GetMapping("/{schoolId}")
    public GuardianRegistrationSettingsResponse get(
            @PathVariable Long schoolId
    ) {
        return guardianRegistrationService.getSettings(schoolId);
    }

    @PutMapping("/{schoolId}")
    public GuardianRegistrationSettingsResponse update(
            @PathVariable Long schoolId,
            @Valid @RequestBody UpdateGuardianRegistrationSettingsRequest request
    ) {
        return guardianRegistrationService.updateSettings(schoolId, request);
    }

    @PostMapping("/{schoolId}/rotate-token")
    public GuardianRegistrationSettingsResponse rotateToken(
            @PathVariable Long schoolId
    ) {
        return guardianRegistrationService.rotateToken(schoolId);
    }
}
