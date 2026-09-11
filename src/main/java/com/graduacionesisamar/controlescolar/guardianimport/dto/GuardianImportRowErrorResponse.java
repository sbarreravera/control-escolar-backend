package com.graduacionesisamar.controlescolar.guardianimport.dto;

import java.util.List;

/**
 * Describes the validation problems found in one guardian import row.
 */
public record GuardianImportRowErrorResponse(
        Integer rowNumber,
        String externalReference,
        String enrollmentNumber,
        List<String> messages
) {
}
