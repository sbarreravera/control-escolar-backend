package com.graduacionesisamar.controlescolar.guardianactivation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Guardians selected by a school administrator for batch activation.
 */
public record CreateGuardianInvitationsRequest(
        @NotNull(message = "School id is required")
        Long schoolId,

        @NotEmpty(message = "Select at least one guardian")
        @Size(
                max = 2000,
                message = "No more than 2000 guardians can be invited at once"
        )
        List<@NotNull Long> guardianIds
) {
}
