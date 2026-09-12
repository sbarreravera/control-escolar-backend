package com.graduacionesisamar.controlescolar.guardian.dto;

public record GuardianDeletionStudentResponse(
        Long studentId,
        String studentName,
        String enrollmentNumber
) {
}
