package com.graduacionesisamar.controlescolar.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contains the information that can be updated for a student.
 */
public record UpdateStudentRequest(

        @NotBlank(message = "Enrollment number is required")
        @Size(max = 50)
        String enrollmentNumber,

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 150)
        String lastName,

        @NotNull(message = "School group id is required")
        Long schoolGroupId
) {
}
