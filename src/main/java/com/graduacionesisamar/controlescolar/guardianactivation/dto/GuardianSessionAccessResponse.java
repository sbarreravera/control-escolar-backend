package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import java.time.OffsetDateTime;

/**
 * One active guardian session shown to an authorized school administrator.
 */
public record GuardianSessionAccessResponse(
        Long sessionId,
        String deviceName,
        boolean notificationsEnabled,
        OffsetDateTime createdAt,
        OffsetDateTime lastUsedAt,
        OffsetDateTime expiresAt
) {
}
