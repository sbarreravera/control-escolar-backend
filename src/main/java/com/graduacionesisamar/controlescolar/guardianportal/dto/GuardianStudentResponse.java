package com.graduacionesisamar.controlescolar.guardianportal.dto;

/**
 * A student related to the guardian represented by the active session.
 */
public record GuardianStudentResponse(
        Long studentId,
        String enrollmentNumber,
        String fullName,
        boolean active,
        String relationship,
        boolean primaryContact,
        Long academicCycleId,
        String academicCycleName,
        Long schoolGroupId,
        String gradeName,
        String groupName,
        GuardianAccessEventResponse latestEvent
) {
}
