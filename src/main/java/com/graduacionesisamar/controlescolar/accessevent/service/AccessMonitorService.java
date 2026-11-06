package com.graduacionesisamar.controlescolar.accessevent.service;

import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.schoolgroup.repository.SchoolGroupRepository;
import com.graduacionesisamar.controlescolar.accessevent.dto.AccessMonitorFilterOptions;
import com.graduacionesisamar.controlescolar.accessevent.dto.AccessMonitorEventResponse;
import com.graduacionesisamar.controlescolar.accessevent.dto.AccessMonitorPageResponse;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEvent;
import com.graduacionesisamar.controlescolar.accessevent.entity.AccessEventType;
import com.graduacionesisamar.controlescolar.accessevent.repository.AccessEventRepository;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read-only history separated from the scan and notification workflow.
 * Access is scoped by both the authenticated user's school and query school.
 */
@Service
@RequiredArgsConstructor
public class AccessMonitorService {
    private static final ZoneId SCHOOL_TIME_ZONE =
            ZoneId.of("America/Mexico_City");

    private final AccessEventRepository accessEventRepository;
    private final SchoolAccessService schoolAccessService;
    private final AcademicCycleRepository academicCycleRepository;
    private final SchoolGroupRepository schoolGroupRepository;

    @Transactional(readOnly = true)
    public AccessMonitorFilterOptions findFilterOptions(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);

        var cycles = academicCycleRepository
                .findAllBySchool_IdOrderByStartDateDesc(schoolId)
                .stream()
                .map(cycle -> new AccessMonitorFilterOptions.CycleOption(
                        cycle.getId(), cycle.getName(),
                        Boolean.TRUE.equals(cycle.getActive())
                ))
                .toList();

        var groups = schoolGroupRepository
                .findAllByAcademicCycle_School_IdOrderByGradeNameAscGroupNameAsc(schoolId)
                .stream()
                .map(group -> new AccessMonitorFilterOptions.GroupOption(
                        group.getId(), group.getAcademicCycle().getId(),
                        group.getGradeName(), group.getGroupName(),
                        Boolean.TRUE.equals(group.getActive())
                ))
                .toList();

        return new AccessMonitorFilterOptions(cycles, groups);
    }

    @Transactional(readOnly = true)
    public AccessMonitorPageResponse findPage(
            Long schoolId,
            LocalDate date,
            int page,
            int size,
            String search,
            Long academicCycleId,
            Long schoolGroupId,
            AccessEventType eventType
    ) {
        schoolAccessService.requireAccessToSchool(schoolId);

        DayWindow day = windowFor(date);
        String normalizedSearch = search == null
                ? ""
                : search.strip().replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);

        Specification<AccessEvent> specification = (root, query, cb) -> {
            Join<AccessEvent, Student> student =
                    root.join("student", JoinType.INNER);
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.equal(student.get("school").get("id"), schoolId));
            filters.add(cb.greaterThanOrEqualTo(
                    root.get("occurredAt"), day.start()
            ));
            filters.add(cb.lessThan(root.get("occurredAt"), day.end()));

            if (eventType != null) {
                filters.add(cb.equal(root.get("eventType"), eventType));
            }
            if (academicCycleId != null) {
                filters.add(cb.equal(
                        student.get("schoolGroup").get("academicCycle").get("id"),
                        academicCycleId
                ));
            }
            if (schoolGroupId != null) {
                filters.add(cb.equal(
                        student.get("schoolGroup").get("id"),
                        schoolGroupId
                ));
            }
            if (!normalizedSearch.isEmpty()) {
                String match = "%" + escapeLike(normalizedSearch) + "%";
                var first = cb.lower(student.get("firstName"));
                var last = cb.lower(student.get("lastName"));
                filters.add(cb.or(
                        cb.like(cb.lower(student.get("enrollmentNumber")),
                                match, '\\'),
                        cb.like(first, match, '\\'),
                        cb.like(last, match, '\\'),
                        cb.like(cb.concat(cb.concat(first, " "), last),
                                match, '\\'),
                        cb.like(cb.concat(cb.concat(last, " "), first),
                                match, '\\')
                ));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };

        var sort = Sort.by(Sort.Direction.DESC, "occurredAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));

        Page<AccessEvent> records = accessEventRepository.findAll(
                specification,
                PageRequest.of(page, size, sort)
        );

        return new AccessMonitorPageResponse(
                date,
                records.getContent().stream().map(this::toResponse).toList(),
                records.getNumber(),
                records.getSize(),
                records.getTotalElements(),
                records.getTotalPages(),
                records.isFirst(),
                records.isLast()
        );
    }

    private static String escapeLike(String search) {
        return search.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    static DayWindow windowFor(LocalDate date) {
        OffsetDateTime start =
                date.atStartOfDay(SCHOOL_TIME_ZONE).toOffsetDateTime();
        OffsetDateTime end =
                date.plusDays(1).atStartOfDay(SCHOOL_TIME_ZONE).toOffsetDateTime();
        return new DayWindow(start, end);
    }

    record DayWindow(OffsetDateTime start, OffsetDateTime end) {}

    private AccessMonitorEventResponse toResponse(AccessEvent event) {
        Student student = event.getStudent();
        SchoolGroup group = student.getSchoolGroup();
        return new AccessMonitorEventResponse(
                event.getId(),
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getEnrollmentNumber(),
                group == null ? null : group.getAcademicCycle().getName(),
                student.getGradeName(),
                student.getGroupName(),
                event.getEventType(),
                event.getCaptureMethod(),
                event.getOccurredAt(),
                event.getDeviceName()
        );
    }
}
