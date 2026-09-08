package com.graduacionesisamar.controlescolar.academiccycle.service;

import com.graduacionesisamar.controlescolar.academiccycle.dto.AcademicCycleResponse;
import com.graduacionesisamar.controlescolar.academiccycle.dto.CreateAcademicCycleRequest;
import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
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
 * Handles business operations related to academic cycles.
 */
@Service
@RequiredArgsConstructor
public class AcademicCycleService {

    private final AcademicCycleRepository academicCycleRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolAccessService schoolAccessService;

    /**
     * Creates an academic cycle for a school.
     */
    @Transactional
    public AcademicCycleResponse create(CreateAcademicCycleRequest request) {
        schoolAccessService.requireAccessToSchool(request.schoolId());
        School school = findSchool(request.schoolId());
        String name = normalize(request.name());

        validateDates(request);
        validateName(request.schoolId(), name);

        AcademicCycle cycle = new AcademicCycle();
        cycle.setSchool(school);
        cycle.setName(name);
        cycle.setStartDate(request.startDate());
        cycle.setEndDate(request.endDate());

        return toResponse(academicCycleRepository.save(cycle));
    }

    /**
     * Returns all academic cycles belonging to a school.
     */
    @Transactional(readOnly = true)
    public List<AcademicCycleResponse> findAllBySchool(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);
        findSchool(schoolId);

        return academicCycleRepository
                .findAllBySchool_IdOrderByStartDateDesc(schoolId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private School findSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School not found"
                ));
    }

    private void validateDates(CreateAcademicCycleRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date must be on or after start date"
            );
        }
    }

    private void validateName(Long schoolId, String name) {
        if (academicCycleRepository
                .existsBySchool_IdAndNameIgnoreCase(schoolId, name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An academic cycle with this name already exists"
            );
        }
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private AcademicCycleResponse toResponse(AcademicCycle cycle) {
        return new AcademicCycleResponse(
                cycle.getId(),
                cycle.getSchool().getId(),
                cycle.getSchool().getName(),
                cycle.getName(),
                cycle.getStartDate(),
                cycle.getEndDate(),
                cycle.getActive(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt()
        );
    }
}
