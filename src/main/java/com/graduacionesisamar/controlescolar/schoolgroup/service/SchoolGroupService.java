package com.graduacionesisamar.controlescolar.schoolgroup.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.CreateSchoolGroupRequest;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.SchoolGroupResponse;
import com.graduacionesisamar.controlescolar.schoolgroup.dto.UpdateSchoolGroupRequest;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.schoolgroup.repository.SchoolGroupRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Handles business operations related to school groups.
 */
@Service
@RequiredArgsConstructor
public class SchoolGroupService {

    private final SchoolGroupRepository schoolGroupRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final StudentRepository studentRepository;
    private final SchoolAccessService schoolAccessService;

    /**
     * Creates a group within an academic cycle.
     */
    @Transactional
    public SchoolGroupResponse create(CreateSchoolGroupRequest request) {
        AcademicCycle cycle = findCycle(request.academicCycleId());
        schoolAccessService.requireAccessToSchool(cycle.getSchool().getId());

        String gradeName = normalize(request.gradeName());
        String groupName = normalize(request.groupName())
                .toUpperCase(Locale.ROOT);

        validateDuplicate(request.academicCycleId(), gradeName, groupName);

        SchoolGroup group = new SchoolGroup();
        group.setAcademicCycle(cycle);
        group.setGradeName(gradeName);
        group.setGroupName(groupName);

        return toResponse(schoolGroupRepository.save(group));
    }

    /**
     * Returns all groups belonging to an academic cycle.
     */
    @Transactional(readOnly = true)
    public List<SchoolGroupResponse> findAllByAcademicCycle(
            Long academicCycleId
    ) {
        AcademicCycle cycle = findCycle(academicCycleId);
        schoolAccessService.requireAccessToSchool(cycle.getSchool().getId());

        return schoolGroupRepository
                .findAllByAcademicCycle_IdOrderByGradeNameAscGroupNameAsc(
                        academicCycleId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Corrects the grade or group name without changing its academic cycle.
     */
    @Transactional
    public SchoolGroupResponse update(
            Long schoolGroupId,
            UpdateSchoolGroupRequest request
    ) {
        SchoolGroup group = findGroup(schoolGroupId);
        AcademicCycle cycle = group.getAcademicCycle();
        schoolAccessService.requireAccessToSchool(cycle.getSchool().getId());

        String gradeName = normalize(request.gradeName());
        String groupName = normalize(request.groupName())
                .toUpperCase(Locale.ROOT);

        boolean duplicate = schoolGroupRepository
                .existsByAcademicCycle_IdAndGradeNameIgnoreCaseAndGroupNameIgnoreCaseAndIdNot(
                        cycle.getId(),
                        gradeName,
                        groupName,
                        schoolGroupId
                );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This grade and group already exist in the academic cycle"
            );
        }

        group.setGradeName(gradeName);
        group.setGroupName(groupName);

        return toResponse(schoolGroupRepository.save(group));
    }

    /**
     * Deletes an empty group. Groups with students must be preserved.
     */
    @Transactional
    public void delete(Long schoolGroupId) {
        SchoolGroup group = findGroup(schoolGroupId);
        schoolAccessService.requireAccessToSchool(
                group.getAcademicCycle().getSchool().getId()
        );

        if (studentRepository.existsBySchoolGroup_Id(schoolGroupId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "School group cannot be deleted while it has students assigned"
            );
        }

        schoolGroupRepository.delete(group);
    }

    private AcademicCycle findCycle(Long academicCycleId) {
        return academicCycleRepository.findById(academicCycleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Academic cycle not found"
                ));
    }

    private SchoolGroup findGroup(Long schoolGroupId) {
        return schoolGroupRepository.findById(schoolGroupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School group not found"
                ));
    }

    private void validateDuplicate(
            Long academicCycleId,
            String gradeName,
            String groupName
    ) {
        boolean exists = schoolGroupRepository
                .existsByAcademicCycle_IdAndGradeNameIgnoreCaseAndGroupNameIgnoreCase(
                        academicCycleId,
                        gradeName,
                        groupName
                );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This grade and group already exist in the academic cycle"
            );
        }
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private SchoolGroupResponse toResponse(SchoolGroup group) {
        AcademicCycle cycle = group.getAcademicCycle();

        return new SchoolGroupResponse(
                group.getId(),
                cycle.getSchool().getId(),
                cycle.getId(),
                cycle.getName(),
                group.getGradeName(),
                group.getGroupName(),
                group.getActive(),
                studentRepository.existsBySchoolGroup_Id(group.getId()),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
