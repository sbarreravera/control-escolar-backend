package com.graduacionesisamar.controlescolar.studentimport.dto;

import java.util.List;

/**
 * Summarizes a student import spreadsheet validation.
 */
public record StudentImportValidationResponse(
        int totalRows,
        int validRows,
        int invalidRows,
        boolean canImport,
        List<StudentImportRowErrorResponse> errors
) {
}
