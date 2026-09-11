package com.graduacionesisamar.controlescolar.guardianimport.dto;

import java.util.List;

/**
 * Summarizes a guardian and relationship workbook validation.
 */
public record GuardianImportValidationResponse(
        int totalRows,
        int validRows,
        int invalidRows,
        int guardiansToCreate,
        int guardiansToReuse,
        int relationshipsToCreate,
        boolean canImport,
        List<GuardianImportRowErrorResponse> errors
) {
}
