package com.graduacionesisamar.controlescolar.guardianpasswordreset.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public password recovery request for a guardian account.
 */
public record GuardianPasswordResetRequest(
        @NotBlank
        @Size(max = 50)
        String schoolCode,

        @NotBlank
        @Email
        @Size(max = 150)
        String email
) {
}
