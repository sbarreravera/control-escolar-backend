package com.graduacionesisamar.controlescolar.guardianpasswordreset.event;

import java.time.OffsetDateTime;

/**
 * Data required to deliver a one-time guardian password reset link.
 */
public record GuardianPasswordResetRequestedEvent(
        String recipientEmail,
        String guardianName,
        String schoolName,
        String schoolCode,
        String username,
        String resetToken,
        OffsetDateTime expiresAt
) {
}
