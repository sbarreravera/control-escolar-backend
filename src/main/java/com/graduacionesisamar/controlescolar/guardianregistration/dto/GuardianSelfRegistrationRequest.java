package com.graduacionesisamar.controlescolar.guardianregistration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GuardianSelfRegistrationRequest(
        String registrationToken,
        String schoolCode,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 50) String relationship,
        @NotEmpty List<@NotBlank @Size(max = 50) String> studentEnrollmentNumbers,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
