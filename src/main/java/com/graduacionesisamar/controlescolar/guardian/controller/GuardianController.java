package com.graduacionesisamar.controlescolar.guardian.controller;

import com.graduacionesisamar.controlescolar.guardian.dto.CreateGuardianRequest;
import com.graduacionesisamar.controlescolar.guardian.dto.GuardianDeletionImpactResponse;
import com.graduacionesisamar.controlescolar.guardian.dto.GuardianResponse;
import com.graduacionesisamar.controlescolar.guardian.dto.UpdateGuardianRequest;
import com.graduacionesisamar.controlescolar.guardian.service.GuardianDeletionService;
import com.graduacionesisamar.controlescolar.guardian.service.GuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes REST operations for guardian management.
 */
@RestController
@RequestMapping("/api/v1/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;
    private final GuardianDeletionService guardianDeletionService;

    /**
     * Registers a new guardian.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardianResponse create(
            @Valid @RequestBody CreateGuardianRequest request
    ) {
        return guardianService.create(request);
    }

    /**
     * Returns all guardians belonging to a school.
     */
    @GetMapping
    public List<GuardianResponse> findAllBySchool(
            @RequestParam Long schoolId
    ) {
        return guardianService.findAllBySchool(schoolId);
    }

    /**
     * Returns a guardian by identifier.
     */
    @GetMapping("/{id}")
    public GuardianResponse findById(@PathVariable Long id) {
        return guardianService.findById(id);
    }

    /**
     * Shows the school-scoped consequences before permanently deleting a guardian.
     */
    @GetMapping("/{id}/deletion-impact")
    public GuardianDeletionImpactResponse deletionImpact(
            @PathVariable Long id
    ) {
        return guardianDeletionService.preview(id);
    }

    /**
     * Updates the editable personal information of a guardian.
     */
    @PutMapping("/{id}")
    public GuardianResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGuardianRequest request
    ) {
        return guardianService.update(id, request);
    }

    /**
     * Permanently removes a guardian and all guardian-owned portal/push data.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        guardianDeletionService.delete(id);
    }
}
