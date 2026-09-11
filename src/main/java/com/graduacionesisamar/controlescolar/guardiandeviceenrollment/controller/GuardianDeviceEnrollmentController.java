package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.controller;

import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentRequest;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CreateGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.GuardianInvitationStatusResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service.CompletedGuardianEnrollment;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service.GuardianDeviceEnrollmentService;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianSessionCookieService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes operations for securely enrolling guardian devices.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GuardianDeviceEnrollmentController {

    private final GuardianDeviceEnrollmentService enrollmentService;
    private final GuardianSessionCookieService cookieService;

    @GetMapping("/guardian-device-enrollments/status")
    public GuardianInvitationStatusResponse status(
            @RequestParam String token
    ) {
        return enrollmentService.findStatus(token);
    }

    /**
     * Creates a temporary invitation for a guardian.
     * This operation requires an authenticated school user.
     */
    @PostMapping(
            "/guardians/{guardianId}/device-enrollments"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGuardianDeviceEnrollmentResponse create(
            @PathVariable Long guardianId
    ) {
        return enrollmentService.create(guardianId);
    }

    /**
     * Completes an invitation from the guardian's device.
     */
    @PostMapping(
            "/guardian-device-enrollments/complete"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public CompleteGuardianDeviceEnrollmentResponse complete(
            @Valid @RequestBody
            CompleteGuardianDeviceEnrollmentRequest request,
            HttpServletResponse response
    ) {
        CompletedGuardianEnrollment completed =
                enrollmentService.complete(request);

        cookieService.write(
                response,
                completed.sessionToken(),
                completed.response().sessionExpiresAt()
        );

        return completed.response();
    }
}
