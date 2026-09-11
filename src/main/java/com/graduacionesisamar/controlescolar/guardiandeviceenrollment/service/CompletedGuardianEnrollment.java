package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service;

import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentResponse;

/**
 * Internal completion result carrying the raw token only as far as the
 * controller that writes the HttpOnly cookie.
 */
public record CompletedGuardianEnrollment(
        CompleteGuardianDeviceEnrollmentResponse response,
        String sessionToken
) {
}
