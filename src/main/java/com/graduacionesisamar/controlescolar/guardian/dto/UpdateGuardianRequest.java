package com.graduacionesisamar.controlescolar.guardian.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contains the editable personal information of an existing guardian.
 */
public record UpdateGuardianRequest(

        @NotBlank(message = "Guardian full name is required")
        @Size(max = 150, message = "Full name cannot exceed 150 characters")
        String fullName,

        @Size(max = 30, message = "Phone cannot exceed 30 characters")
        String phone,

        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        String email
) {
}
