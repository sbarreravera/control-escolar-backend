package com.graduacionesisamar.controlescolar.accessevent.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Filtered, paginated access history for one local school day.
 */
public record AccessMonitorPageResponse(
        LocalDate date,
        List<AccessMonitorEventResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}
