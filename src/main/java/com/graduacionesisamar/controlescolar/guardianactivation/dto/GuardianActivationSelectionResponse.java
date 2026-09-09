package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import java.util.List;

/**
 * Guardian identifiers matching the complete activation filter.
 */
public record GuardianActivationSelectionResponse(
        List<Long> guardianIds,
        int totalSelected
) {
}
