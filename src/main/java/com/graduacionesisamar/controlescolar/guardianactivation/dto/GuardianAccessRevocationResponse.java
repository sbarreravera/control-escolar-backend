package com.graduacionesisamar.controlescolar.guardianactivation.dto;

/**
 * Summary of invitations, sessions and devices revoked in one operation.
 */
public record GuardianAccessRevocationResponse(
        int guardiansProcessed,
        int invitationsRevoked,
        int sessionsRevoked,
        int devicesDeactivated
) {
}
