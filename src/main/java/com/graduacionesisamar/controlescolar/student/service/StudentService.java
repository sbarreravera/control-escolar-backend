package com.graduacionesisamar.controlescolar.student.service;

import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.student.dto.CreateStudentRequest;
import com.graduacionesisamar.controlescolar.student.dto.StudentResponse;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Handles business operations related to students.
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;

    /**
     * Creates a new student.
     */
    public StudentResponse create(CreateStudentRequest request) {
        School school = findSchool(request.schoolId());
        String enrollment = request.enrollmentNumber().trim().toUpperCase();

        validateEnrollment(request.schoolId(), enrollment);

        Student student = buildStudent(request, school, enrollment);
        return toResponse(studentRepository.save(student));
    }

    /**
     * Returns the students registered in a school.
     */
    @Transactional(readOnly = true)
    public List<StudentResponse> findAllBySchool(Long schoolId) {
        findSchool(schoolId);

        return studentRepository
                .findAllBySchool_IdOrderByLastNameAscFirstNameAsc(schoolId)
                .stream()
                .map(this::toResponse)
                .toList();
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

        return toResponse(student);
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

    private Student buildStudent(
            CreateStudentRequest request,
            School school,
            String enrollment
    ) {
        Student student = new Student();
        student.setSchool(school);
        student.setEnrollmentNumber(enrollment);
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setGradeName(trimNullable(request.gradeName()));
        student.setGroupName(trimNullable(request.groupName()));
        return student;
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }

    private StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getSchool().getId(),
                student.getSchool().getName(),
                student.getEnrollmentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getGradeName(),
                student.getGroupName(),
                student.getActive(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}