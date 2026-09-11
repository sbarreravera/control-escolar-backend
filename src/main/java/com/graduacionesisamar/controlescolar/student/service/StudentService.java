package com.graduacionesisamar.controlescolar.student.service;

import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.schoolgroup.entity.SchoolGroup;
import com.graduacionesisamar.controlescolar.schoolgroup.repository.SchoolGroupRepository;
import com.graduacionesisamar.controlescolar.student.dto.CreateStudentRequest;
import com.graduacionesisamar.controlescolar.student.dto.StudentResponse;
import com.graduacionesisamar.controlescolar.student.dto.StudentPageResponse;
import com.graduacionesisamar.controlescolar.student.dto.UpdateStudentRequest;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Handles business operations related to students.
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolGroupRepository schoolGroupRepository;
    private final SchoolAccessService schoolAccessService;

    /**
     * Creates a new student.
     */
    @Transactional
    public StudentResponse create(CreateStudentRequest request) {
        schoolAccessService.requireAccessToSchool(request.schoolId());
        School school = findSchool(request.schoolId());
        SchoolGroup schoolGroup = resolveSchoolGroup(
                request.schoolGroupId(),
                request.schoolId()
        );
        String enrollment = request.enrollmentNumber().trim().toUpperCase();

        validateEnrollment(request.schoolId(), enrollment);

        Student student = buildStudent(
                request,
                school,
                schoolGroup,
                enrollment
        );
        return toResponse(studentRepository.save(student));
    }

    /**
     * Returns the students registered in a school.
     */
    @Transactional(readOnly = true)
    public List<StudentResponse> findAllBySchool(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);
        findSchool(schoolId);

        return studentRepository
                .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(schoolId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a filtered and paginated school roster.
     */
    @Transactional(readOnly = true)
    public StudentPageResponse findPageBySchool(
            Long schoolId,
            int page,
            int size,
            String search,
            Long academicCycleId,
            Long schoolGroupId,
            Boolean active,
            String sort,
            String direction
    ) {
        schoolAccessService.requireAccessToSchool(schoolId);
        findSchool(schoolId);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                buildSort(sort, direction)
        );

        Page<Student> result = studentRepository.searchBySchool(
                schoolId,
                normalizeSearch(search),
                academicCycleId,
                schoolGroupId,
                active,
                pageable
        );

        return new StudentPageResponse(
                result.getContent().stream()
                        .map(this::toResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    /**
     * Returns a student by identifier.
     */
    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        schoolAccessService.requireAccessToSchool(
                student.getSchool().getId()
        );

        return toResponse(student);
    }

    /**
     * Updates a student without allowing transfers between schools.
     */
    @Transactional
    public StudentResponse update(
            Long id,
            UpdateStudentRequest request
    ) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        Long schoolId = student.getSchool().getId();
        schoolAccessService.requireAccessToSchool(schoolId);

        SchoolGroup schoolGroup = resolveSchoolGroup(
                request.schoolGroupId(),
                schoolId
        );
        String enrollment = request.enrollmentNumber()
                .trim()
                .toUpperCase();

        validateEnrollmentForUpdate(
                schoolId,
                enrollment,
                student.getId()
        );

        student.setEnrollmentNumber(enrollment);
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setSchoolGroup(schoolGroup);
        student.setGradeName(schoolGroup.getGradeName());
        student.setGroupName(schoolGroup.getGroupName());

        return toResponse(studentRepository.save(student));
    }

    private School findSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School not found"
                ));
    }

    private void validateEnrollment(Long schoolId, String enrollment) {
        boolean exists = studentRepository
                .existsBySchool_IdAndEnrollmentNumberIgnoreCase(
                        schoolId,
                        enrollment
                );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The enrollment number already exists in this school"
            );
        }
    }

    private void validateEnrollmentForUpdate(
            Long schoolId,
            String enrollment,
            Long studentId
    ) {
        boolean exists = studentRepository
                .existsBySchool_IdAndEnrollmentNumberIgnoreCaseAndIdNot(
                        schoolId,
                        enrollment,
                        studentId
                );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The enrollment number already exists in this school"
            );
        }
    }

    private Student buildStudent(
            CreateStudentRequest request,
            School school,
            SchoolGroup schoolGroup,
            String enrollment
    ) {
        Student student = new Student();
        student.setSchool(school);
        student.setEnrollmentNumber(enrollment);
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setSchoolGroup(schoolGroup);

        if (schoolGroup == null) {
            student.setGradeName(trimNullable(request.gradeName()));
            student.setGroupName(trimNullable(request.groupName()));
        } else {
            student.setGradeName(schoolGroup.getGradeName());
            student.setGroupName(schoolGroup.getGroupName());
        }

        return student;
    }

    private SchoolGroup resolveSchoolGroup(
            Long schoolGroupId,
            Long schoolId
    ) {
        if (schoolGroupId == null) {
            return null;
        }

        SchoolGroup group = schoolGroupRepository.findById(schoolGroupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School group not found"
                ));

        Long groupSchoolId = group.getAcademicCycle().getSchool().getId();

        if (!Objects.equals(groupSchoolId, schoolId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "School group does not belong to the selected school"
            );
        }

        if (!Boolean.TRUE.equals(group.getActive())
                || !Boolean.TRUE.equals(group.getAcademicCycle().getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "School group is not active"
            );
        }

        return group;
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeSearch(String search) {
        return search == null
                ? ""
                : search.trim()
                        .replaceAll("\\s+", " ")
                        .toLowerCase(Locale.ROOT);
    }

    private Sort buildSort(String sort, String direction) {
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Sort primarySort = switch (sort == null ? "" : sort) {
            case "enrollmentNumber" -> Sort.by(
                    sortDirection,
                    "enrollmentNumber"
            );
            case "academicCycleName" -> Sort.by(
                    sortDirection,
                    "schoolGroup.academicCycle.name"
            );
            case "schoolGroup" -> Sort.by(
                    sortDirection,
                    "gradeName",
                    "groupName"
            );
            case "active" -> Sort.by(sortDirection, "active");
            default -> Sort.by(
                    sortDirection,
                    "lastName",
                    "firstName"
            );
        };

        return primarySort.and(Sort.by("id"));
    }

    private StudentResponse toResponse(Student student) {
        SchoolGroup group = student.getSchoolGroup();

        return new StudentResponse(
                student.getId(),
                student.getSchool().getId(),
                student.getSchool().getName(),
                student.getEnrollmentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getGradeName(),
                student.getGroupName(),
                group == null ? null : group.getId(),
                group == null ? null : group.getAcademicCycle().getId(),
                group == null ? null : group.getAcademicCycle().getName(),
                student.getActive(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
