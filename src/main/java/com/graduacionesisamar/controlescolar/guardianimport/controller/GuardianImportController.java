package com.graduacionesisamar.controlescolar.guardianimport.controller;

import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportResultResponse;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportTemplate;
import com.graduacionesisamar.controlescolar.guardianimport.dto.GuardianImportValidationResponse;
import com.graduacionesisamar.controlescolar.guardianimport.service.GuardianImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Exposes the official guardian and relationship import workflow.
 */
@RestController
@RequestMapping("/api/v1/guardian-imports")
@RequiredArgsConstructor
public class GuardianImportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final GuardianImportService guardianImportService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(
            @RequestParam Long schoolId
    ) {
        GuardianImportTemplate template = guardianImportService
                .generateTemplate(schoolId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(template.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(template.content());
    }

    @PostMapping(
            path = "/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public GuardianImportValidationResponse validate(
            @RequestParam Long schoolId,
            @RequestParam MultipartFile file
    ) {
        return guardianImportService.validate(schoolId, file);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GuardianImportResultResponse> importGuardians(
            @RequestParam Long schoolId,
            @RequestParam MultipartFile file
    ) {
        GuardianImportResultResponse result = guardianImportService
                .importGuardians(schoolId, file);

        HttpStatus status = result.validation().canImport()
                ? HttpStatus.CREATED
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(result);
    }
}
