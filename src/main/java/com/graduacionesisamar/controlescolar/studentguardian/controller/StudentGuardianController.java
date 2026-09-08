package com.graduacionesisamar.controlescolar.studentguardian.controller;

import com.graduacionesisamar.controlescolar.studentguardian.dto.CreateStudentGuardianRequest;
import com.graduacionesisamar.controlescolar.studentguardian.dto.StudentGuardianResponse;
import com.graduacionesisamar.controlescolar.studentguardian.service.StudentGuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes REST operations for student-guardian relationships.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StudentGuardianController {

    private final StudentGuardianService studentGuardianService;

    /**
     * Links a guardian to a student.
     */
    @PostMapping("/student-guardians")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentGuardianResponse create(
            @Valid @RequestBody CreateStudentGuardianRequest request
    ) {
        return studentGuardianService.create(request);
    }

    /**
     * Returns the guardians associated with a student.
     */
    @GetMapping("/students/{studentId}/guardians")
    public List<StudentGuardianResponse> findAllByStudent(
            @PathVariable Long studentId
    ) {
        return studentGuardianService.findAllByStudent(studentId);
    }

    /**
     * Removes a guardian from a student.
     */
    @DeleteMapping(
            "/students/{studentId}/guardians/{guardianId}"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long studentId,
            @PathVariable Long guardianId
    ) {
        studentGuardianService.delete(studentId, guardianId);
    }
}