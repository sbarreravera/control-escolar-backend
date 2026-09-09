package com.graduacionesisamar.controlescolar.guardianimport.dto;

/**
 * Contains a generated guardian import workbook and its download name.
 */
public record GuardianImportTemplate(
        String fileName,
        byte[] content
) {
}
