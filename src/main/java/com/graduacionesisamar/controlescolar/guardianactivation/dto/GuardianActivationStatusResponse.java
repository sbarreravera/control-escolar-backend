package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import java.time.OffsetDateTime;

/**
 * Administrative activation state without exposing invitation tokens.
 */
public record GuardianActivationStatusResponse(
        Long guardianId,
        String externalReference,
        String guardianName,
        String phone,
        String email,
        boolean guardianActive,
        String activationState,
        OffsetDateTime invitationCreatedAt,
        OffsetDateTime invitationExpiresAt,
        OffsetDateTime activatedAt,
        int activeDevices,
        int activeSessions
) {
}
