package com.graduacionesisamar.controlescolar.schoolgroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contains the information required to create a school group.
 */
public record CreateSchoolGroupRequest(

        @NotNull(message = "Academic cycle id is required")
        Long academicCycleId,

        @NotBlank(message = "Grade name is required")
        @Size(max = 50)
        String gradeName,

        @NotBlank(message = "Group name is required")
        @Size(max = 50)
        String groupName
) {
}
