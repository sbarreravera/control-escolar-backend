package com.graduacionesisamar.controlescolar.guardianaccount.dto;

import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;

import java.time.OffsetDateTime;

/**
 * Guardian identity returned after a successful credential login.
 */
public record GuardianLoginResponse(
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
