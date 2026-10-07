package com.graduacionesisamar.controlescolar.schoolgroup.controller;

import com.graduacionesisamar.controlescolar.security.module.SchoolModule;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.CreateSchoolGroupRequest;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.SchoolGroupResponse;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.UpdateSchoolGroupRequest;
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
@SchoolModule(
        key = "ACADEMIC_STRUCTURE",
        name = "Administración escolar",
        description = "Administrar ciclos escolares, grados y grupos.",
        defaultGranted = false,
        order = 80
)
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

    @PutMapping("/{schoolGroupId}")
    public SchoolGroupResponse update(
            @PathVariable Long schoolGroupId,
            @Valid @RequestBody UpdateSchoolGroupRequest request
    ) {
        return schoolGroupService.update(schoolGroupId, request);
    }

    @DeleteMapping("/{schoolGroupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long schoolGroupId) {
        schoolGroupService.delete(schoolGroupId);
    }
}
