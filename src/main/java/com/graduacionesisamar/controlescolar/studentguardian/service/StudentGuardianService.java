package com.graduacionesisamar.controlescolar.studentguardian.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import com.graduacionesisamar.controlescolar.studentguardian.dto.CreateStudentGuardianRequest;
import com.graduacionesisamar.controlescolar.studentguardian.dto.StudentGuardianResponse;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardianId;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentGuardianService {

    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final SchoolAccessService schoolAccessService;

    public StudentGuardianResponse create(
            CreateStudentGuardianRequest request
    ) {
        Student student = findStudent(request.studentId());
        schoolAccessService.requireAccessToSchool(
                student.getSchool().getId()
        );

        Guardian guardian = findGuardian(request.guardianId());
        schoolAccessService.requireAccessToSchool(
                guardian.getSchool().getId()
        );

        validateRelationship(request, student, guardian);

        StudentGuardian association = buildAssociation(
                request,
                student,
                guardian
        );

        return toResponse(studentGuardianRepository.save(association));
    }

    @Transactional(readOnly = true)
    public List<StudentGuardianResponse> findAllByStudent(Long studentId) {
        Student student = findStudent(studentId);

        schoolAccessService.requireAccessToSchool(
                student.getSchool().getId()
        );

        return studentGuardianRepository
                .findAllByStudent_IdOrderByPrimaryContactDescGuardian_FullNameAsc(
                        studentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(Long studentId, Long guardianId) {
        StudentGuardianId id = new StudentGuardianId(
                studentId,
                guardianId
        );

        StudentGuardian association = studentGuardianRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student-guardian relationship not found"
                ));

        schoolAccessService.requireAccessToSchool(
                association.getStudent().getSchool().getId()
        );

        studentGuardianRepository.delete(association);
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));
    }

    private Guardian findGuardian(Long guardianId) {
        return guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian not found"
                ));
    }

    private void validateRelationship(
            CreateStudentGuardianRequest request,
            Student student,
            Guardian guardian
    ) {
        validateSameSchool(student, guardian);
        validateDuplicate(request.studentId(), request.guardianId());
        validatePrimaryContact(request);
    }

    private void validateSameSchool(
            Student student,
            Guardian guardian
    ) {
        Long studentSchoolId = student.getSchool().getId();
        Long guardianSchoolId = guardian.getSchool().getId();

        if (!studentSchoolId.equals(guardianSchoolId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student and guardian must belong to the same school"
            );
        }
    }

    private void validateDuplicate(
            Long studentId,
            Long guardianId
    ) {
        boolean exists = studentGuardianRepository
                .existsByStudent_IdAndGuardian_Id(studentId, guardianId);

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Guardian is already linked to this student"
            );
        }
    }

    private void validatePrimaryContact(
            CreateStudentGuardianRequest request
    ) {
        if (!Boolean.TRUE.equals(request.primaryContact())) {
            return;
        }

        boolean exists = studentGuardianRepository
                .existsByStudent_IdAndPrimaryContactTrue(
                        request.studentId()
                );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student already has a primary contact"
            );
        }
    }

    private StudentGuardian buildAssociation(
            CreateStudentGuardianRequest request,
            Student student,
            Guardian guardian
    ) {
        StudentGuardian association = new StudentGuardian();

        association.setId(new StudentGuardianId(
                student.getId(),
                guardian.getId()
        ));
        association.setStudent(student);
        association.setGuardian(guardian);
        association.setRelationship(trimNullable(request.relationship()));
        association.setPrimaryContact(
                Boolean.TRUE.equals(request.primaryContact())
        );
        association.setReceivesNotifications(
                request.receivesNotifications() == null
                        || request.receivesNotifications()
        );

        return association;
    }

    private String trimNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private StudentGuardianResponse toResponse(
            StudentGuardian association
    ) {
        Student student = association.getStudent();
        Guardian guardian = association.getGuardian();

        return new StudentGuardianResponse(
                student.getId(),
                buildStudentName(student),
                guardian.getId(),
                guardian.getFullName(),
                association.getRelationship(),
                association.getPrimaryContact(),
                association.getReceivesNotifications(),
                association.getCreatedAt()
        );
    }

    private String buildStudentName(Student student) {
        return "%s %s".formatted(
                student.getFirstName(),
                student.getLastName()
        );
    }
}