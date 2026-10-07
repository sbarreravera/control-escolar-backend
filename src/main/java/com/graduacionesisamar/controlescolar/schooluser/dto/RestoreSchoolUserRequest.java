package com.graduacionesisamar.controlescolar.schooluser.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestoreSchoolUserRequest(
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
