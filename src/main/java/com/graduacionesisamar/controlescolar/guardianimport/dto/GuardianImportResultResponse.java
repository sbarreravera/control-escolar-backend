package com.graduacionesisamar.controlescolar.guardianimport.dto;

/**
 * Returns the result of an attempted guardian and relationship import.
 */
public record GuardianImportResultResponse(
        int guardiansCreated,
        int guardiansReused,
        int relationshipsCreated,
        GuardianImportValidationResponse validation
) {
}
