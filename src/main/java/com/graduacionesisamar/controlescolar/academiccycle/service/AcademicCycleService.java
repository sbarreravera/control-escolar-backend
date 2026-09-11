package com.graduacionesisamar.controlescolar.academiccycle.service;

import com.graduacionesisamar.controlescolar.academiccycle.dto.AcademicCycleResponse;
import com.graduacionesisamar.controlescolar.academiccycle.dto.CreateAcademicCycleRequest;
import com.graduacionesisamar.controlescolar.academiccycle.dto.UpdateAcademicCycleRequest;
import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.schoolgroup.repository.SchoolGroupRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Handles business operations related to academic cycles.
 */
@Service
@RequiredArgsConstructor
public class AcademicCycleService {

    private final AcademicCycleRepository academicCycleRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolGroupRepository schoolGroupRepository;
    private final SchoolAccessService schoolAccessService;

    /**
     * Creates an academic cycle for a school.
     */
    @Transactional
    public AcademicCycleResponse create(CreateAcademicCycleRequest request) {
        schoolAccessService.requireAccessToSchool(request.schoolId());
        School school = findSchool(request.schoolId());
        String name = normalize(request.name());

        validateDates(request.startDate(), request.endDate());
        validateName(request.schoolId(), name);

        AcademicCycle cycle = new AcademicCycle();
        cycle.setSchool(school);
        cycle.setName(name);
        cycle.setStartDate(request.startDate());
        cycle.setEndDate(request.endDate());

        return toResponse(academicCycleRepository.save(cycle));
    }

    /**
     * Updates editable information without changing the school or deleting
     * existing academic structure.
     */
    @Transactional
    public AcademicCycleResponse update(
            Long academicCycleId,
            UpdateAcademicCycleRequest request
    ) {
        AcademicCycle cycle = findAcademicCycle(academicCycleId);
        Long schoolId = cycle.getSchool().getId();

        schoolAccessService.requireAccessToSchool(schoolId);

        String name = normalize(request.name());
        validateDates(request.startDate(), request.endDate());
        validateNameForUpdate(schoolId, name, academicCycleId);

        cycle.setName(name);
        cycle.setStartDate(request.startDate());
        cycle.setEndDate(request.endDate());

        return toResponse(academicCycleRepository.save(cycle));
    }

    /**
     * Deletes an empty academic cycle. A cycle with at least one grade/group
     * is protected to avoid orphaning or destroying academic structure.
     */
    @Transactional
    public void delete(Long academicCycleId) {
        AcademicCycle cycle = findAcademicCycle(academicCycleId);
        Long schoolId = cycle.getSchool().getId();

        schoolAccessService.requireAccessToSchool(schoolId);

        if (schoolGroupRepository.existsByAcademicCycle_Id(academicCycleId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Academic cycle has grades or groups associated"
            );
        }

        academicCycleRepository.delete(cycle);
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

    private AcademicCycle findAcademicCycle(Long academicCycleId) {
        return academicCycleRepository.findById(academicCycleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Academic cycle not found"
                ));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
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

    private void validateNameForUpdate(
            Long schoolId,
            String name,
            Long academicCycleId
    ) {
        if (academicCycleRepository
                .existsBySchool_IdAndNameIgnoreCaseAndIdNot(
                        schoolId,
                        name,
                        academicCycleId
                )) {
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
        boolean hasGroups = cycle.getId() != null
                && schoolGroupRepository.existsByAcademicCycle_Id(cycle.getId());

        return new AcademicCycleResponse(
                cycle.getId(),
                cycle.getSchool().getId(),
                cycle.getSchool().getName(),
                cycle.getName(),
                cycle.getStartDate(),
                cycle.getEndDate(),
                cycle.getActive(),
                hasGroups,
                cycle.getCreatedAt(),
                cycle.getUpdatedAt()
        );
    }
}
