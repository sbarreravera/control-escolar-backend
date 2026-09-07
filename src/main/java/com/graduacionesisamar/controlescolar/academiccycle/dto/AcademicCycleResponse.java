package com.graduacionesisamar.controlescolar.academiccycle.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Represents academic cycle information returned by the API.
 */
public record AcademicCycleResponse(
        Long id,
        Long schoolId,
        String schoolName,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
