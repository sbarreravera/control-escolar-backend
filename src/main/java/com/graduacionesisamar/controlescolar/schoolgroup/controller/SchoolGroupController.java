package com.graduacionesisamar.controlescolar.schoolgroup.controller;

import com.graduacionesisamar.controlescolar.schoolgroup.dto.CreateSchoolGroupRequest;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.SchoolGroupResponse;
import com.graduacionesisamar.controlescolar.schoolgroup.service.SchoolGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes REST operations for school group management.
 */
@RestController
@RequestMapping("/api/v1/school-groups")
@RequiredArgsConstructor
public class SchoolGroupController {

    private final SchoolGroupService schoolGroupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolGroupResponse create(
            @Valid @RequestBody CreateSchoolGroupRequest request
    ) {
        return schoolGroupService.create(request);
    }

    @GetMapping
    public List<SchoolGroupResponse> findAllByAcademicCycle(
            @RequestParam Long academicCycleId
    ) {
        return schoolGroupService.findAllByAcademicCycle(academicCycleId);
    }
}
