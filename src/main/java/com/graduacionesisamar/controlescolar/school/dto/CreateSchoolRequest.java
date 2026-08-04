package com.graduacionesisamar.controlescolar.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contains the information required to create a school.
 */
public record CreateSchoolRequest(

        @NotBlank(message = "School name is required")
        @Size(max = 150, message = "School name cannot exceed 150 characters")
        String name,

        @NotBlank(message = "School code is required")
        @Size(max = 50, message = "School code cannot exceed 50 characters")
        String code
) {
}