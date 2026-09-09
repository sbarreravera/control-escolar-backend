package com.graduacionesisamar.controlescolar.guardianactivation.dto;

/**
 * Activation totals for the selected academic scope before state filtering.
 */
public record GuardianActivationSummaryResponse(
        long totalGuardians,
        long notInvited,
        long pending,
        long active,
        long requiresActivation,
        long missingContact
) {
}
