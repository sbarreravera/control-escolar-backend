package com.graduacionesisamar.controlescolar.school.controller;

import com.graduacionesisamar.controlescolar.school.dto.CreateSchoolRequest;
import com.graduacionesisamar.controlescolar.school.dto.SchoolResponse;
import com.graduacionesisamar.controlescolar.school.service.SchoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes REST operations for school management.
 */
@RestController
@RequestMapping("/api/v1/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    /**
     * Registers a new school.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolResponse create(
            @Valid @RequestBody CreateSchoolRequest request
    ) {
        return schoolService.create(request);
    }

    /**
     * Returns all registered schools.
     */
    @GetMapping
    public List<SchoolResponse> findAll() {
        return schoolService.findAll();
    }
}