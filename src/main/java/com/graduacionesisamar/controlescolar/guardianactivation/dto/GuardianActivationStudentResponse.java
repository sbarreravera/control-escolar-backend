package com.graduacionesisamar.controlescolar.guardianactivation.dto;

/**
 * Active student related to a guardian in the selected academic cycle.
 */
public record GuardianActivationStudentResponse(
        Long studentId,
        String enrollmentNumber,
        String fullName,
        Long schoolGroupId,
        String gradeName,
        String groupName
) {
}
