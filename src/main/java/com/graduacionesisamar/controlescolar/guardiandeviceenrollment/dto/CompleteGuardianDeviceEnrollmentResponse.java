package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto;

import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;

import java.time.OffsetDateTime;

/**
 * Public confirmation returned after activation. The session token is sent
 * exclusively through an HttpOnly cookie.
 */
public record CompleteGuardianDeviceEnrollmentResponse(
        Long guardianId,
        String guardianName,
        String schoolName,
        String schoolCode,
        String username,
        GuardianDeviceResponse device,
        boolean notificationsEnabled,
        OffsetDateTime sessionExpiresAt
) {
}
