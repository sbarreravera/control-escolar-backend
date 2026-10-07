package com.graduacionesisamar.controlescolar.schooluser.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateSchoolUserRequest(
        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        boolean active,

        @Size(min = 8, max = 72)
        String password,

        @NotNull
        Set<
                @NotBlank
                @Size(max = 100)
                String
        > moduleKeys
) {
}
