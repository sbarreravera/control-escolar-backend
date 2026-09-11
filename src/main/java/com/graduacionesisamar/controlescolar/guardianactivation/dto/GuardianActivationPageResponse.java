package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import java.util.List;

/**
 * One filtered page of guardian activation statuses.
 */
public record GuardianActivationPageResponse(
        List<GuardianActivationStatusResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        GuardianActivationSummaryResponse summary
) {
}
