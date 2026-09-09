package com.graduacionesisamar.controlescolar.guardiansession.security;

import java.time.OffsetDateTime;

/**
 * Trusted identity reconstructed from an opaque guardian session.
 */
public record GuardianPrincipal(
        Long sessionId,
        Long guardianId,
        Long guardianDeviceId,
        Long schoolId,
        String guardianName,
        String schoolName,
        OffsetDateTime expiresAt
) {
}
