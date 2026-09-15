package com.graduacionesisamar.controlescolar.guardianregistration.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateGuardianRegistrationSettingsRequest(
        @NotNull Boolean enabled,
        @NotNull @Min(0) Integer minimumGuardiansPerStudent,
        @NotNull @Min(1) Integer maximumGuardiansPerStudent
) {
}
