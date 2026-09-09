package com.graduacionesisamar.controlescolar.guardianactivation.controller;

import com.graduacionesisamar.controlescolar.guardianactivation.dto.CreateGuardianInvitationsRequest;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianAccessRevocationResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationPageResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationSelectionResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianInvitationBatchResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.RevokeGuardianAccessRequest;
import com.graduacionesisamar.controlescolar.guardianactivation.service.GuardianActivationAdministrationService;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service.GuardianDeviceEnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public GuardianActivationPageResponse findAll(
            @RequestParam Long schoolId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NOT_ACTIVE") String state,
            @RequestParam Long academicCycleId,
            @RequestParam(required = false) Long schoolGroupId,
            @RequestParam(defaultValue = "") String gradeName,
            @RequestParam(defaultValue = "ALL") String contact
    ) {
        return administrationService.findPage(
                schoolId,
                page,
                size,
                search,
                state,
                academicCycleId,
                schoolGroupId,
                gradeName,
                contact
        );
    }

    @GetMapping("/selection")
    public GuardianActivationSelectionResponse findSelection(
            @RequestParam Long schoolId,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NOT_ACTIVE") String state,
            @RequestParam Long academicCycleId,
            @RequestParam(required = false) Long schoolGroupId,
            @RequestParam(defaultValue = "") String gradeName,
            @RequestParam(defaultValue = "ALL") String contact
    ) {
        return administrationService.findSelection(
                schoolId,
                search,
                state,
                academicCycleId,
                schoolGroupId,
                gradeName,
                contact
        );
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
