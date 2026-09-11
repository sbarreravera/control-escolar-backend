package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Administrative activation state without exposing invitation tokens.
 */
public record GuardianActivationStatusResponse(
        Long guardianId,
        String externalReference,
        String guardianName,
        String phone,
        String email,
        String username,
        boolean accountActivated,
        boolean guardianActive,
        String activationState,
        OffsetDateTime invitationCreatedAt,
        OffsetDateTime invitationExpiresAt,
        OffsetDateTime activatedAt,
        int activeDevices,
        int activeSessions,
        List<GuardianActivationStudentResponse> students
) {
}
