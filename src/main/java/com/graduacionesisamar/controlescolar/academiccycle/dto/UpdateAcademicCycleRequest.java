package com.graduacionesisamar.controlescolar.academiccycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Contains editable academic cycle information.
 */
public record UpdateAcademicCycleRequest(

        @NotBlank(message = "Academic cycle name is required")
        @Size(max = 50)
        String name,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {
}
