package com.graduacionesisamar.controlescolar.student.controller;

import com.graduacionesisamar.controlescolar.student.dto.CreateStudentRequest;
import com.graduacionesisamar.controlescolar.student.dto.StudentResponse;
import com.graduacionesisamar.controlescolar.student.dto.StudentPageResponse;
import com.graduacionesisamar.controlescolar.student.dto.UpdateStudentRequest;
import com.graduacionesisamar.controlescolar.student.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes REST operations for student management.
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Validated
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
     * Returns a searchable page for the student administration screen.
     */
    @GetMapping("/page")
    public StudentPageResponse findPageBySchool(
            @RequestParam Long schoolId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25")
            @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long academicCycleId,
            @RequestParam(required = false) Long schoolGroupId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "studentName") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return studentService.findPageBySchool(
                schoolId,
                page,
                size,
                search,
                academicCycleId,
                schoolGroupId,
                active,
                sort,
                direction
        );
    }

    /**
     * Returns a student by identifier.
     */
    @GetMapping("/{id}")
    public StudentResponse findById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    /**
     * Updates a student and their assigned school group.
     */
    @PutMapping("/{id}")
    public StudentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request
    ) {
        return studentService.update(id, request);
    }
}
