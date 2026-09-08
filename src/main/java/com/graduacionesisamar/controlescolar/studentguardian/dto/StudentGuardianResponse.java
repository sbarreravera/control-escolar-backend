package com.graduacionesisamar.controlescolar.studentguardian.dto;

import java.time.OffsetDateTime;

/**
 * Represents a student-guardian relationship returned by the API.
 */
public record StudentGuardianResponse(
        Long studentId,
        String studentName,
        Long guardianId,
        String guardianName,
        String relationship,
        Boolean primaryContact,
        Boolean receivesNotifications,
        OffsetDateTime createdAt
) {
}