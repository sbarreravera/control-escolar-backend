package com.graduacionesisamar.controlescolar.student.dto;

import java.time.OffsetDateTime;

/**
 * Represents student information returned by the API.
 */
public record StudentResponse(
        Long id,
        Long schoolId,
        String schoolName,
        String enrollmentNumber,
        String firstName,
        String lastName,
        String gradeName,
        String groupName,
        Long schoolGroupId,
        Long academicCycleId,
        String academicCycleName,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
