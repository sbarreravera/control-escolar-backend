package com.graduacionesisamar.controlescolar.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contains the information required to create a school
 * together with its first administrator.
 */
public record CreateSchoolRequest(

        @NotBlank(message = "School name is required")
        @Size(max = 150, message = "School name cannot exceed 150 characters")
        String name,

        @NotBlank(message = "School code is required")
        @Size(max = 50, message = "School code cannot exceed 50 characters")
        String code,

        @NotBlank(message = "Administrator name is required")
        @Size(max = 150, message = "Administrator name cannot exceed 150 characters")
        String adminFullName,

        @NotBlank(message = "Administrator email is required")
        @Email(message = "Administrator email must be valid")
        @Size(max = 150, message = "Administrator email cannot exceed 150 characters")
        String adminEmail,

        @NotBlank(message = "Administrator password is required")
        @Size(
                min = 8,
                max = 72,
                message = "Administrator password must contain between 8 and 72 characters"
        )
        String adminPassword
) {
}