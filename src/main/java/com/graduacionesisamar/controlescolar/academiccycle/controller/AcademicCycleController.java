package com.graduacionesisamar.controlescolar.academiccycle.controller;

import com.graduacionesisamar.controlescolar.academiccycle.dto.AcademicCycleResponse;
import com.graduacionesisamar.controlescolar.academiccycle.dto.CreateAcademicCycleRequest;
import com.graduacionesisamar.controlescolar.academiccycle.service.AcademicCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes REST operations for academic cycle management.
 */
@RestController
@RequestMapping("/api/v1/academic-cycles")
@RequiredArgsConstructor
public class AcademicCycleController {

    private final AcademicCycleService academicCycleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicCycleResponse create(
            @Valid @RequestBody CreateAcademicCycleRequest request
    ) {
        return academicCycleService.create(request);
    }

    @GetMapping
    public List<AcademicCycleResponse> findAllBySchool(
            @RequestParam Long schoolId
    ) {
        return academicCycleService.findAllBySchool(schoolId);
    }
}
