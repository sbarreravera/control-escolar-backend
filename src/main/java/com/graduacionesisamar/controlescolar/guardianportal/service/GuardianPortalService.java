package com.graduacionesisamar.controlescolar.guardianportal.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.repository.AccessEventRepository;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianAccessEventPageResponse;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianAccessEventResponse;
import com.graduacionesisamar.controlescolar.guardianportal.dto.GuardianStudentResponse;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only guardian portal operations scoped from the trusted session.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianPortalService {

    private final StudentGuardianRepository studentGuardianRepository;
    private final AccessEventRepository accessEventRepository;

    public List<GuardianStudentResponse> findStudents(
            GuardianPrincipal principal
    ) {
        return studentGuardianRepository.findForGuardianPortal(
                        principal.guardianId(),
                        principal.schoolId()
                )
                .stream()
                .map(this::toStudentResponse)
                .toList();
    }

    public GuardianAccessEventPageResponse findHistory(
            GuardianPrincipal principal,
            int page,
            int size,
            Long studentId,
            AccessEventType eventType,
            OffsetDateTime occurredFrom,
            OffsetDateTime occurredTo
    ) {
        validateDateRange(occurredFrom, occurredTo);

        Page<AccessEvent> result = accessEventRepository
                .findGuardianHistory(
                        principal.guardianId(),
                        principal.schoolId(),
                        studentId,
                        eventType,
                        occurredFrom,
                        occurredTo,
                        PageRequest.of(page, size)
                );

        return new GuardianAccessEventPageResponse(
                result.getContent().stream()
                        .map(this::toEventResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    public GuardianAccessEventResponse findEvent(
            GuardianPrincipal principal,
            Long eventId
    ) {
        return accessEventRepository.findGuardianEvent(
                        eventId,
                        principal.guardianId(),
                        principal.schoolId()
                )
                .map(this::toEventResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Access event not found"
                ));
    }

    private GuardianStudentResponse toStudentResponse(
            StudentGuardian relationship
    ) {
        Student student = relationship.getStudent();
        SchoolGroup schoolGroup = student.getSchoolGroup();
        AcademicCycle academicCycle = schoolGroup == null
                ? null
                : schoolGroup.getAcademicCycle();

        GuardianAccessEventResponse latestEvent = accessEventRepository
                .findFirstByStudent_IdOrderByOccurredAtDescIdDesc(
                        student.getId()
                )
                .map(this::toEventResponse)
                .orElse(null);

        return new GuardianStudentResponse(
                student.getId(),
                student.getEnrollmentNumber(),
                buildStudentName(student),
                Boolean.TRUE.equals(student.getActive()),
                relationship.getRelationship(),
                Boolean.TRUE.equals(relationship.getPrimaryContact()),
                academicCycle == null ? null : academicCycle.getId(),
                academicCycle == null ? null : academicCycle.getName(),
                schoolGroup == null ? null : schoolGroup.getId(),
                schoolGroup == null
                        ? student.getGradeName()
                        : schoolGroup.getGradeName(),
                schoolGroup == null
                        ? student.getGroupName()
                        : schoolGroup.getGroupName(),
                latestEvent
        );
    }

    private GuardianAccessEventResponse toEventResponse(
            AccessEvent event
    ) {
        Student student = event.getStudent();

        return new GuardianAccessEventResponse(
                event.getId(),
                student.getId(),
                buildStudentName(student),
                student.getEnrollmentNumber(),
                event.getEventType(),
                event.getOccurredAt()
        );
    }

    private String buildStudentName(Student student) {
        return "%s %s".formatted(
                student.getFirstName(),
                student.getLastName()
        ).trim();
    }

    private void validateDateRange(
            OffsetDateTime occurredFrom,
            OffsetDateTime occurredTo
    ) {
        if (occurredFrom != null
                && occurredTo != null
                && !occurredFrom.isBefore(occurredTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The start date must be before the end date"
            );
        }
    }
}
