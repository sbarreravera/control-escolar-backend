package com.graduacionesisamar.controlescolar.studentimport.controller;

import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportResultResponse;
import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportTemplate;
import com.graduacionesisamar.controlescolar.studentimport.dto.StudentImportValidationResponse;
import com.graduacionesisamar.controlescolar.studentimport.service.StudentImportService;
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
 * Exposes the official workbook and the initial student import workflow.
 */
@RestController
@RequestMapping("/api/v1/student-imports")
@RequiredArgsConstructor
public class StudentImportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final StudentImportService studentImportService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(
            @RequestParam Long academicCycleId
    ) {
        StudentImportTemplate template = studentImportService
                .generateTemplate(academicCycleId);

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
    public StudentImportValidationResponse validate(
            @RequestParam Long academicCycleId,
            @RequestParam MultipartFile file
    ) {
        return studentImportService.validate(academicCycleId, file);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentImportResultResponse> importStudents(
            @RequestParam Long academicCycleId,
            @RequestParam MultipartFile file
    ) {
        StudentImportResultResponse result = studentImportService
                .importStudents(academicCycleId, file);

        HttpStatus status = result.validation().canImport()
                ? HttpStatus.CREATED
                : HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(result);
    }
}
