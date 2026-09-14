package com.graduacionesisamar.controlescolar.guardianregistration.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardianaccount.service.GuardianAccountService;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianRegistrationContextResponse;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianRegistrationSchoolResponse;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianRegistrationSettingsResponse;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianSelfRegistrationRequest;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.GuardianSelfRegistrationResponse;
import com.graduacionesisamar.controlescolar.guardianregistration.dto.UpdateGuardianRegistrationSettingsRequest;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardianId;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class GuardianRegistrationService {

    private static final int MAX_STUDENTS_PER_REGISTRATION = 10;
    private static final int MAX_REFERENCE_SEQUENCE = 999;

    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final GuardianAccountService guardianAccountService;
    private final SchoolAccessService schoolAccessService;

    @Transactional(readOnly = true)
    public GuardianRegistrationSettingsResponse getSettings(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);
        return toSettingsResponse(findSchool(schoolId));
    }

    public GuardianRegistrationSettingsResponse updateSettings(
            Long schoolId,
            UpdateGuardianRegistrationSettingsRequest request
    ) {
        schoolAccessService.requireAccessToSchool(schoolId);
        School school = findSchool(schoolId);
        validateRange(
                request.minimumGuardiansPerStudent(),
                request.maximumGuardiansPerStudent()
        );
        school.setGuardianSelfRegistrationEnabled(request.enabled());
        school.setGuardianMinPerStudent(request.minimumGuardiansPerStudent());
        school.setGuardianMaxPerStudent(request.maximumGuardiansPerStudent());
        return toSettingsResponse(schoolRepository.save(school));
    }

    public GuardianRegistrationSettingsResponse rotateToken(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);
        School school = findSchool(schoolId);
        school.rotateGuardianRegistrationToken();
        return toSettingsResponse(schoolRepository.save(school));
    }

    @Transactional(readOnly = true)
    public List<GuardianRegistrationSchoolResponse> listAvailableSchools() {
        return schoolRepository
                .findAllByActiveTrueAndGuardianSelfRegistrationEnabledTrueOrderByNameAsc()
                .stream()
                .map(school -> new GuardianRegistrationSchoolResponse(
                        school.getName(),
                        school.getCode()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public GuardianRegistrationContextResponse resolveContext(String token) {
        School school = findRegistrationSchoolByToken(token);
        return toContextResponse(school);
    }

    public GuardianSelfRegistrationResponse register(
            GuardianSelfRegistrationRequest request
    ) {
        School school = resolveRegistrationSchool(request);
        validateContact(request.phone(), request.email());

        List<String> enrollmentNumbers = normalizeEnrollmentNumbers(
                request.studentEnrollmentNumbers()
        );
        List<Student> students = lockAndValidateStudents(
                school,
                enrollmentNumbers
        );

        Guardian guardian = new Guardian();
        guardian.setSchool(school);
        guardian.setExternalReference(generateGuardianReference(
                school,
                students.getFirst().getEnrollmentNumber()
        ));
        guardian.setFullName(request.fullName().trim());
        guardian.setPhone(trimNullable(request.phone()));
        guardian.setEmail(normalizeEmail(request.email()));
        guardian.setActive(true);
        Guardian savedGuardian = guardianRepository.saveAndFlush(guardian);

        GuardianAccount account = guardianAccountService.ensureAccount(
                savedGuardian
        );
        account = guardianAccountService.setPassword(
                account,
                request.password()
        );

        String relationship = request.relationship().trim();
        List<StudentGuardian> relationships = new ArrayList<>();
        for (Student student : students) {
            long currentGuardianCount = studentGuardianRepository
                    .countByStudent_Id(student.getId());

            StudentGuardian link = new StudentGuardian();
            link.setId(new StudentGuardianId(
                    student.getId(),
                    savedGuardian.getId()
            ));
            link.setStudent(student);
            link.setGuardian(savedGuardian);
            link.setRelationship(relationship);
            link.setPrimaryContact(currentGuardianCount == 0);
            link.setReceivesNotifications(true);
            relationships.add(link);
        }
        studentGuardianRepository.saveAll(relationships);

        return new GuardianSelfRegistrationResponse(
                savedGuardian.getId(),
                savedGuardian.getExternalReference(),
                account.getUsername(),
                school.getName(),
                school.getCode(),
                students.stream()
                        .map(Student::getEnrollmentNumber)
                        .toList()
        );
    }

    private School resolveRegistrationSchool(
            GuardianSelfRegistrationRequest request
    ) {
        if (request.registrationToken() != null
                && !request.registrationToken().isBlank()) {
            return findRegistrationSchoolByToken(
                    request.registrationToken().trim()
            );
        }

        if (request.schoolCode() == null || request.schoolCode().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "School registration link or school code is required"
            );
        }

        School school = schoolRepository
                .findByCodeIgnoreCase(request.schoolCode().trim())
                .orElseThrow(this::registrationSchoolNotFound);
        requireSelfRegistrationAvailable(school);
        return school;
    }

    private School findRegistrationSchoolByToken(String token) {
        if (token == null || token.isBlank()) {
            throw registrationSchoolNotFound();
        }
        School school = schoolRepository
                .findByGuardianRegistrationToken(token.trim())
                .orElseThrow(this::registrationSchoolNotFound);
        requireSelfRegistrationAvailable(school);
        return school;
    }

    private void requireSelfRegistrationAvailable(School school) {
        if (!Boolean.TRUE.equals(school.getActive())
                || !Boolean.TRUE.equals(
                        school.getGuardianSelfRegistrationEnabled()
                )) {
            throw registrationSchoolNotFound();
        }
    }

    private List<String> normalizeEnrollmentNumbers(List<String> values) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                unique.add(value.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (unique.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one student enrollment number is required"
            );
        }
        if (unique.size() > MAX_STUDENTS_PER_REGISTRATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Too many students in one registration"
            );
        }
        return unique.stream().sorted().toList();
    }

    private List<Student> lockAndValidateStudents(
            School school,
            List<String> enrollmentNumbers
    ) {
        List<Student> students = new ArrayList<>();
        for (String enrollmentNumber : enrollmentNumbers) {
            Student student = studentRepository
                    .findBySchoolAndEnrollmentNumberForUpdate(
                            school.getId(),
                            enrollmentNumber
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Student enrollment number was not found in this school"
                    ));

            if (!Boolean.TRUE.equals(student.getActive())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Student is not active"
                );
            }

            long current = studentGuardianRepository.countByStudent_Id(
                    student.getId()
            );
            if (current >= school.getGuardianMaxPerStudent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Student already reached the maximum number of guardians allowed by the school"
                );
            }
            students.add(student);
        }
        return students;
    }

    private String generateGuardianReference(
            School school,
            String studentEnrollmentNumber
    ) {
        String enrollment = studentEnrollmentNumber
                .trim()
                .toUpperCase(Locale.ROOT);
        for (int sequence = 1; sequence <= MAX_REFERENCE_SEQUENCE; sequence++) {
            String suffix = "-T%02d".formatted(sequence);
            int maxBaseLength = 50 - suffix.length();
            String base = enrollment.length() <= maxBaseLength
                    ? enrollment
                    : enrollment.substring(0, maxBaseLength);
            String candidate = base + suffix;
            if (!guardianRepository
                    .existsBySchool_IdAndExternalReferenceIgnoreCase(
                            school.getId(),
                            candidate
                    )) {
                return candidate;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Could not generate an available guardian reference"
        );
    }

    private void validateContact(String phone, String email) {
        if ((phone == null || phone.isBlank())
                && (email == null || email.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone or email is required"
            );
        }
    }

    private void validateRange(int minimum, int maximum) {
        if (maximum < minimum) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum guardians per student must be greater than or equal to the minimum"
            );
        }
    }

    private GuardianRegistrationSettingsResponse toSettingsResponse(
            School school
    ) {
        return new GuardianRegistrationSettingsResponse(
                school.getId(),
                school.getName(),
                school.getGuardianSelfRegistrationEnabled(),
                school.getGuardianMinPerStudent(),
                school.getGuardianMaxPerStudent(),
                school.getGuardianRegistrationToken()
        );
    }

    private GuardianRegistrationContextResponse toContextResponse(
            School school
    ) {
        return new GuardianRegistrationContextResponse(
                school.getName(),
                school.getCode(),
                school.getGuardianMinPerStudent(),
                school.getGuardianMaxPerStudent()
        );
    }

    private School findSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School not found"
                ));
    }

    private String trimNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException registrationSchoolNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Guardian self-registration is not available for this school"
        );
    }
}
