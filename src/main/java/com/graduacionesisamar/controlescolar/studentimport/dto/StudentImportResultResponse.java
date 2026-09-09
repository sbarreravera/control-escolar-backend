package com.graduacionesisamar.controlescolar.studentimport.dto;

/**
 * Returns the result of an attempted student import.
 */
public record StudentImportResultResponse(
        int importedRows,
        StudentImportValidationResponse validation
) {
}
