package com.graduacionesisamar.controlescolar.guardianportal.dto;

import java.util.List;

/**
 * A page of guardian-visible access events.
 */
public record GuardianAccessEventPageResponse(
        List<GuardianAccessEventResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
