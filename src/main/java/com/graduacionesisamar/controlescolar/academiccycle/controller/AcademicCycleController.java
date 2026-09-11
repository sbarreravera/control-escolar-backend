package com.graduacionesisamar.controlescolar.academiccycle.controller;

import com.graduacionesisamar.controlescolar.academiccycle.dto.AcademicCycleResponse;
import com.graduacionesisamar.controlescolar.academiccycle.dto.CreateAcademicCycleRequest;
import com.graduacionesisamar.controlescolar.academiccycle.dto.UpdateAcademicCycleRequest;
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

    @PutMapping("/{academicCycleId}")
    public AcademicCycleResponse update(
            @PathVariable Long academicCycleId,
            @Valid @RequestBody UpdateAcademicCycleRequest request
    ) {
        return academicCycleService.update(academicCycleId, request);
    }

    @DeleteMapping("/{academicCycleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long academicCycleId) {
        academicCycleService.delete(academicCycleId);
    }

    @GetMapping
    public List<AcademicCycleResponse> findAllBySchool(
            @RequestParam Long schoolId
    ) {
        return academicCycleService.findAllBySchool(schoolId);
    }
}
