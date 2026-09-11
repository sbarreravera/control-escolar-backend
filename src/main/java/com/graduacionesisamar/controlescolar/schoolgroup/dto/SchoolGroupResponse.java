package com.graduacionesisamar.controlescolar.schoolgroup.dto;

import java.time.OffsetDateTime;

/**
 * Represents school group information returned by the API.
 */
public record SchoolGroupResponse(
        Long id,
        Long schoolId,
        Long academicCycleId,
        String academicCycleName,
        String gradeName,
        String groupName,
        Boolean active,
        Boolean hasStudents,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
