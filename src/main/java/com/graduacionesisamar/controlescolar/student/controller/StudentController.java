package com.graduacionesisamar.controlescolar.student.controller;

import com.graduacionesisamar.controlescolar.student.dto.CreateStudentRequest;
import com.graduacionesisamar.controlescolar.student.dto.StudentResponse;
import com.graduacionesisamar.controlescolar.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes REST operations for student management.
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    /**
     * Registers a new student.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(
            @Valid @RequestBody CreateStudentRequest request
    ) {
        return studentService.create(request);
    }

    /**
     * Returns all students belonging to a school.
     */
    @GetMapping
    public List<StudentResponse> findAllBySchool(
            @RequestParam Long schoolId
    ) {
        return studentService.findAllBySchool(schoolId);
    }

    /**
     * Returns a student by identifier.
     */
    @GetMapping("/{id}")
    public StudentResponse findById(@PathVariable Long id) {
        return studentService.findById(id);
    }
}