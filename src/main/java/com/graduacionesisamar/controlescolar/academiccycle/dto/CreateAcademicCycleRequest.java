package com.graduacionesisamar.controlescolar.academiccycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Contains the information required to create an academic cycle.
 */
public record CreateAcademicCycleRequest(

        @NotNull(message = "School id is required")
        Long schoolId,

        @NotBlank(message = "Academic cycle name is required")
        @Size(max = 50)
        String name,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {
}
