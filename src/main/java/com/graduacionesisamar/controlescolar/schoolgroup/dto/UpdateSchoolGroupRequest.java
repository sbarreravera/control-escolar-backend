package com.graduacionesisamar.controlescolar.schoolgroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contains the information that can be corrected for a school group.
 */
public record UpdateSchoolGroupRequest(

        @NotBlank(message = "Grade name is required")
        @Size(max = 50)
        String gradeName,

        @NotBlank(message = "Group name is required")
        @Size(max = 50)
        String groupName
) {
}
