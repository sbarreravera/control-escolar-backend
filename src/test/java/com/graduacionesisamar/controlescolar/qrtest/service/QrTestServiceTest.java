package com.graduacionesisamar.controlescolar.qrtest.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.credential.entity.Credential;
import com.graduacionesisamar.controlescolar.credential.repository.CredentialRepository;
import com.graduacionesisamar.controlescolar.qrtest.dto.QrTestRequest;
import com.graduacionesisamar.controlescolar.qrtest.dto.QrTestResponse;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.student.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrTestServiceTest {
    private static final String TOKEN = "sample-test-token";

    @Mock CredentialRepository credentialRepository;
    @Mock SchoolAccessService schoolAccessService;
    @InjectMocks QrTestService service;

    private Credential credential;
    private Student student;

    @BeforeEach
    void setUp() {
        School school = new School();
        school.setId(44L);
        school.setName("Secundaria Técnica 44");
        student = new Student();
        student.setId(12L);
        student.setSchool(school);
        student.setFirstName("Nombre");
        student.setLastName("Apellido");
        student.setEnrollmentNumber("261B01");
        student.setGradeName("1");
        student.setGroupName("B");
        student.setActive(true);

        credential = new Credential();
        credential.setStudent(student);
        credential.setActive(true);
        credential.setQrToken(TOKEN);
    }

    private void as(AppUserRole role) {
        AppUser user = new AppUser();
        user.setRole(role);
        when(schoolAccessService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void validCredentialOnlyReadsExistingData() {
        as(AppUserRole.SUPER_ADMIN);
        when(credentialRepository.findByQrToken(TOKEN))
                .thenReturn(Optional.of(credential));

        QrTestResponse result = service.validate(
                new QrTestRequest(" " + TOKEN + " ", 44L));

        assertTrue(result.valid());
        assertEquals("VALID", result.status());
        assertEquals("261B01", result.enrollmentNumber());
        assertEquals("1", result.gradeName());
        assertEquals("B", result.groupName());
        verify(schoolAccessService).requireAccessToSchool(44L);
        verify(credentialRepository).findByQrToken(TOKEN);
        verifyNoMoreInteractions(credentialRepository);
    }

    @Test
    void detectsExpiredCredentialWithoutChangingIt() {
        as(AppUserRole.SUPER_ADMIN);
        OffsetDateTime expiry = OffsetDateTime.now().minusDays(1);
        credential.setExpiresAt(expiry);
        when(credentialRepository.findByQrToken(TOKEN))
                .thenReturn(Optional.of(credential));

        QrTestResponse result = service.validate(new QrTestRequest(TOKEN, 44L));

        assertFalse(result.valid());
        assertEquals("EXPIRED", result.status());
        assertTrue(credential.getActive());
        assertEquals(expiry, credential.getExpiresAt());
    }

    @Test
    void detectsDeactivatedCredential() {
        as(AppUserRole.SUPER_ADMIN);
        credential.setActive(false);
        when(credentialRepository.findByQrToken(TOKEN))
                .thenReturn(Optional.of(credential));
        assertEquals("INACTIVE_CREDENTIAL",
                service.validate(new QrTestRequest(TOKEN, 44L)).status());
    }

    @Test
    void detectsInactiveStudent() {
        as(AppUserRole.SUPER_ADMIN);
        student.setActive(false);
        when(credentialRepository.findByQrToken(TOKEN))
                .thenReturn(Optional.of(credential));
        assertEquals("INACTIVE_STUDENT",
                service.validate(new QrTestRequest(TOKEN, 44L)).status());
    }

    @Test
    void rejectsOperatorBeforeLookingUpToken() {
        as(AppUserRole.OPERATOR);
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.validate(new QrTestRequest(TOKEN, 44L)));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(credentialRepository);
    }

    @Test
    void rejectsUnknownQr() {
        as(AppUserRole.SUPER_ADMIN);
        when(credentialRepository.findByQrToken(TOKEN))
                .thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.validate(new QrTestRequest(TOKEN, 44L)));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void doesNotRevealCredentialFromDifferentSchool() {
        as(AppUserRole.SUPER_ADMIN);
        when(credentialRepository.findByQrToken(TOKEN))
                .thenReturn(Optional.of(credential));
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.validate(new QrTestRequest(TOKEN, 17L)));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
