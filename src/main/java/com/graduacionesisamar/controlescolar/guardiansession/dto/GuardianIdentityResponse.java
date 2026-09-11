package com.graduacionesisamar.controlescolar.guardiansession.dto;

import java.time.OffsetDateTime;

/**
 * Guardian identity associated with the current opaque session.
 */
public record GuardianIdentityResponse(
        Long guardianId,
        Long schoolId,
        String guardianName,
        String schoolName,
        String schoolCode,
        String username,
        boolean notificationsEnabled,
        OffsetDateTime sessionExpiresAt
) {
}
