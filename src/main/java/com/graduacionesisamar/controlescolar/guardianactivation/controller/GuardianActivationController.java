package com.graduacionesisamar.controlescolar.guardianactivation.controller;

import com.graduacionesisamar.controlescolar.guardianactivation.dto.CreateGuardianInvitationsRequest;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianAccessRevocationResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationStatusResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianInvitationBatchResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.RevokeGuardianAccessRequest;
import com.graduacionesisamar.controlescolar.guardianactivation.service.GuardianActivationAdministrationService;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service.GuardianDeviceEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administrative guardian activation operations.
 */
@RestController
@RequestMapping("/api/v1/guardian-activations")
@RequiredArgsConstructor
public class GuardianActivationController {

    private final GuardianDeviceEnrollmentService enrollmentService;
    private final GuardianActivationAdministrationService administrationService;

    @GetMapping
    public List<GuardianActivationStatusResponse> findAll(
            @RequestParam Long schoolId
    ) {
        return administrationService.findAll(schoolId);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianInvitationBatchResponse createInvitations(
            @Valid @RequestBody CreateGuardianInvitationsRequest request
    ) {
        return enrollmentService.createBatch(request);
    }

    @PostMapping("/revoke")
    public GuardianAccessRevocationResponse revoke(
            @Valid @RequestBody RevokeGuardianAccessRequest request
    ) {
        return administrationService.revoke(request);
    }
}
