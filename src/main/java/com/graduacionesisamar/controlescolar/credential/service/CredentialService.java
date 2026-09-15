package com.graduacionesisamar.controlescolar.credential.service;

import com.graduacionesisamar.controlescolar.credential.dto.BulkCredentialResponse;
import com.graduacionesisamar.controlescolar.credential.dto.CredentialResponse;
import com.graduacionesisamar.controlescolar.credential.entity.Credential;
import com.graduacionesisamar.controlescolar.credential.repository.CredentialRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CredentialService {

    private static final int TOKEN_SIZE_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CredentialRepository credentialRepository;
    private final StudentRepository studentRepository;
    private final SchoolAccessService schoolAccessService;

    public CredentialResponse create(Long studentId) {
        Student student = findStudentForUpdate(studentId);
        requireStudentSchoolAccess(student);

        validateActiveStudent(student);
        validateNoActiveCredential(studentId);

        return saveCredential(student);
    }

    @Transactional(readOnly = true)
    public CredentialResponse findActive(Long studentId) {
        Student student = findStudent(studentId);
        requireStudentSchoolAccess(student);

        Credential credential = credentialRepository
                .findByStudent_IdAndActiveTrue(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active credential not found"
                ));

        return toResponse(credential);
    }

    /**
     * Returns one active credential for every active student in the school.
     * Existing credentials are preserved so previously printed QR codes keep
     * working; a credential is created only when an active student has none.
     */
    public List<BulkCredentialResponse> ensureActiveForSchool(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);

        List<Student> students = studentRepository
                .findAllBySchool_IdAndActiveTrueOrderByLastNameAscFirstNameAsc(
                        schoolId
                );

        Map<Long, Credential> credentialsByStudentId = credentialRepository
                .findAllByStudent_School_IdAndActiveTrue(schoolId)
                .stream()
                .collect(Collectors.toMap(
                        credential -> credential.getStudent().getId(),
                        Function.identity()
                ));

        List<BulkCredentialResponse> responses =
                new ArrayList<>(students.size());

        for (Student student : students) {
            Credential credential = credentialsByStudentId.get(student.getId());
            boolean created = false;

            if (credential == null) {
                credential = saveCredentialEntity(student);
                created = true;
            }

            responses.add(toBulkResponse(credential, created));
        }

        return responses;
    }

    public void deactivate(Long credentialId) {
        Credential credential = findCredential(credentialId);
        requireStudentSchoolAccess(credential.getStudent());

        if (!Boolean.TRUE.equals(credential.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Credential is already inactive"
            );
        }

        credential.deactivate();
    }

    public CredentialResponse regenerate(Long studentId) {
        Student student = findStudentForUpdate(studentId);
        requireStudentSchoolAccess(student);

        validateActiveStudent(student);
        deactivateActiveCredential(studentId);
        credentialRepository.flush();

        return saveCredential(student);
    }

    private Student findStudentForUpdate(Long studentId) {
        return studentRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));
    }

    private Credential findCredential(Long credentialId) {
        return credentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Credential not found"
                ));
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));
    }

    private void requireStudentSchoolAccess(Student student) {
        schoolAccessService.requireAccessToSchool(
                student.getSchool().getId()
        );
    }

    private void validateActiveStudent(Student student) {
        if (!Boolean.TRUE.equals(student.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student is inactive"
            );
        }
    }

    private void validateNoActiveCredential(Long studentId) {
        if (credentialRepository.existsByStudent_IdAndActiveTrue(studentId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student already has an active credential"
            );
        }
    }

    private void deactivateActiveCredential(Long studentId) {
        credentialRepository
                .findByStudent_IdAndActiveTrue(studentId)
                .ifPresent(Credential::deactivate);
    }

    private CredentialResponse saveCredential(Student student) {
        return toResponse(saveCredentialEntity(student));
    }

    private Credential saveCredentialEntity(Student student) {
        Credential credential = new Credential();
        credential.setStudent(student);
        credential.setQrToken(generateToken());
        return credentialRepository.save(credential);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private CredentialResponse toResponse(Credential credential) {
        return new CredentialResponse(
                credential.getId(),
                credential.getStudent().getId(),
                credential.getQrToken(),
                credential.getActive(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                credential.getDeactivatedAt(),
                credential.getCreatedAt()
        );
    }

    private BulkCredentialResponse toBulkResponse(
            Credential credential,
            boolean created
    ) {
        Student student = credential.getStudent();
        return new BulkCredentialResponse(
                credential.getId(),
                student.getId(),
                student.getEnrollmentNumber(),
                student.getFirstName(),
                student.getLastName(),
                credential.getQrToken(),
                created
        );
    }
}
