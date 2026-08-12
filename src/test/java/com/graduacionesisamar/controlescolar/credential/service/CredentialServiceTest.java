package com.graduacionesisamar.controlescolar.credential.service;

import com.graduacionesisamar.controlescolar.credential.dto.CredentialResponse;
import com.graduacionesisamar.controlescolar.credential.entity.Credential;
import com.graduacionesisamar.controlescolar.credential.repository.CredentialRepository;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import com.graduacionesisamar.controlescolar.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the business rules for student QR credentials.
 */
@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    private static final Long STUDENT_ID = 1L;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CredentialService credentialService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(STUDENT_ID);
        student.setActive(true);
    }

    @Test
    void createReturnsActiveCredentialWithSecureToken() {
        when(studentRepository.findByIdForUpdate(STUDENT_ID))
                .thenReturn(Optional.of(student));
        when(credentialRepository
                .existsByStudent_IdAndActiveTrue(STUDENT_ID))
                .thenReturn(false);
        stubSavedCredential(10L);

        CredentialResponse response = credentialService.create(STUDENT_ID);

        assertEquals(10L, response.id());
        assertEquals(STUDENT_ID, response.studentId());
        assertTrue(response.active());
        assertTrue(response.qrToken()
                .matches("^[A-Za-z0-9_-]{43}$"));
    }

    @Test
    void createRejectsSecondActiveCredential() {
        when(studentRepository.findByIdForUpdate(STUDENT_ID))
                .thenReturn(Optional.of(student));
        when(credentialRepository
                .existsByStudent_IdAndActiveTrue(STUDENT_ID))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> credentialService.create(STUDENT_ID)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void regenerateDeactivatesOldCredentialAndCreatesNewOne() {
        Credential previous = buildActiveCredential(
                11L,
                "previous-token"
        );
        when(studentRepository.findByIdForUpdate(STUDENT_ID))
                .thenReturn(Optional.of(student));
        when(credentialRepository
                .findByStudent_IdAndActiveTrue(STUDENT_ID))
                .thenReturn(Optional.of(previous));
        stubSavedCredential(12L);

        CredentialResponse response =
                credentialService.regenerate(STUDENT_ID);

        assertFalse(previous.getActive());
        assertNotNull(previous.getDeactivatedAt());
        assertEquals(12L, response.id());
        assertNotEquals("previous-token", response.qrToken());
        verify(credentialRepository).flush();
    }

    @Test
    void deactivateMarksCredentialInactive() {
        Credential credential = buildActiveCredential(
                20L,
                "credential-token"
        );
        when(credentialRepository.findById(20L))
                .thenReturn(Optional.of(credential));

        credentialService.deactivate(20L);

        assertFalse(credential.getActive());
        assertNotNull(credential.getDeactivatedAt());
    }

    @Test
    void deactivateRejectsInactiveCredential() {
        Credential credential = buildActiveCredential(
                21L,
                "credential-token"
        );
        credential.deactivate();
        when(credentialRepository.findById(21L))
                .thenReturn(Optional.of(credential));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> credentialService.deactivate(21L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void createRejectsMissingStudent() {
        when(studentRepository.findByIdForUpdate(STUDENT_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> credentialService.create(STUDENT_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(credentialRepository, never()).save(any());
    }

    private void stubSavedCredential(Long credentialId) {
        when(credentialRepository.save(any(Credential.class)))
                .thenAnswer(invocation -> {
                    Credential credential = invocation.getArgument(0);
                    credential.setId(credentialId);
                    credential.beforeInsert();
                    return credential;
                });
    }

    private Credential buildActiveCredential(
            Long credentialId,
            String token
    ) {
        Credential credential = new Credential();
        credential.setId(credentialId);
        credential.setStudent(student);
        credential.setQrToken(token);
        credential.beforeInsert();
        return credential;
    }
}