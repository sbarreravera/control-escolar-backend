package com.graduacionesisamar.controlescolar.studentguardian.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contains the information required to link a guardian to a student.
 */
public record CreateStudentGuardianRequest(

        @NotNull(message = "Student id is required")
        Long studentId,

        @NotNull(message = "Guardian id is required")
        Long guardianId,

        @Size(max = 50, message = "Relationship cannot exceed 50 characters")
        String relationship,

        Boolean primaryContact,

        Boolean receivesNotifications
) {
}