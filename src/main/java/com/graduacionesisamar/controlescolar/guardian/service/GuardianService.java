package com.graduacionesisamar.controlescolar.guardian.service;

import com.graduacionesisamar.controlescolar.guardian.dto.CreateGuardianRequest;
import com.graduacionesisamar.controlescolar.guardian.dto.GuardianResponse;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Handles business operations related to guardians.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolAccessService schoolAccessService;

    /**
     * Creates a new guardian.
     */
    public GuardianResponse create(CreateGuardianRequest request) {
        schoolAccessService.requireAccessToSchool(request.schoolId());
        School school = findSchool(request.schoolId());

        Guardian guardian = buildGuardian(request, school);
        Guardian savedGuardian = guardianRepository.save(guardian);

        return toResponse(savedGuardian);
    }

    /**
     * Returns the guardians registered in a school.
     */
    @Transactional(readOnly = true)
    public List<GuardianResponse> findAllBySchool(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);
        findSchool(schoolId);

        return guardianRepository
                .findAllBySchool_IdOrderByFullNameAsc(schoolId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a guardian by identifier.
     */
    @Transactional(readOnly = true)
    public GuardianResponse findById(Long id) {
        Guardian guardian = guardianRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian not found"
                ));

        schoolAccessService.requireAccessToSchool(
                guardian.getSchool().getId()
        );

        return toResponse(guardian);
    }

    private School findSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School not found"
                ));
    }

    private Guardian buildGuardian(
            CreateGuardianRequest request,
            School school
    ) {
        Guardian guardian = new Guardian();
        guardian.setSchool(school);
        guardian.setFullName(request.fullName().trim());
        guardian.setPhone(trimNullable(request.phone()));
        guardian.setEmail(normalizeEmail(request.email()));
        return guardian;
    }

    private String trimNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = trimNullable(email);
        return normalizedEmail == null
                ? null
                : normalizedEmail.toLowerCase();
    }

    private GuardianResponse toResponse(Guardian guardian) {
        return new GuardianResponse(
                guardian.getId(),
                guardian.getSchool().getId(),
                guardian.getSchool().getName(),
                guardian.getFullName(),
                guardian.getPhone(),
                guardian.getEmail(),
                guardian.getActive(),
                guardian.getCreatedAt(),
                guardian.getUpdatedAt()
        );
    }
}