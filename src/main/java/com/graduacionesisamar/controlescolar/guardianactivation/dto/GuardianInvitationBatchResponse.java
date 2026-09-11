package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Invitations generated atomically for an administrator selection.
 */
public record GuardianInvitationBatchResponse(
        UUID batchId,
        OffsetDateTime expiresAt,
        int invitationsCreated,
        List<GuardianInvitationResponse> invitations
) {
}
