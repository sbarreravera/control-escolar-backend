package com.graduacionesisamar.controlescolar.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contains the information required to create a student.
 */
public record CreateStudentRequest(

        @NotNull(message = "School id is required")
        Long schoolId,

        @NotBlank(message = "Enrollment number is required")
        @Size(max = 50)
        String enrollmentNumber,

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 150)
        String lastName,

        @Size(max = 50)
        String gradeName,

        @Size(max = 50)
        String groupName
) {
}