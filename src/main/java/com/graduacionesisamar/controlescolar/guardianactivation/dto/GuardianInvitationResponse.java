package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import java.time.OffsetDateTime;

/**
 * One generated invitation. The raw token is returned once and never stored.
 */
public record GuardianInvitationResponse(
        Long guardianId,
        String externalReference,
        String guardianName,
        String phone,
        String email,
        String schoolName,
        String enrollmentToken,
        OffsetDateTime expiresAt
) {
}
