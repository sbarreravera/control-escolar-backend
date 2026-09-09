package com.graduacionesisamar.controlescolar.studentimport.dto;

import java.util.List;

/**
 * Describes the validation problems found in one spreadsheet row.
 */
public record StudentImportRowErrorResponse(
        Integer rowNumber,
        String enrollmentNumber,
        List<String> messages
) {
}
