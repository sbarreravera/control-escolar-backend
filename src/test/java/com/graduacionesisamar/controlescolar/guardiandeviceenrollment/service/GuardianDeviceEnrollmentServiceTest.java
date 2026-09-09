package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardianaccount.service.GuardianAccountService;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.CreateGuardianInvitationsRequest;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianInvitationBatchResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentRequest;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CreateGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianDeviceEnrollment;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianEnrollmentPurpose;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.repository.GuardianDeviceEnrollmentRepository;
import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
import com.graduacionesisamar.controlescolar.guardiansession.service.IssuedGuardianSession;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianDeviceEnrollmentServiceTest {

    @Mock
    private GuardianDeviceEnrollmentRepository enrollmentRepository;

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private GuardianDeviceService guardianDeviceService;

    @Mock
    private GuardianSessionService guardianSessionService;

    @Mock
    private SchoolAccessService schoolAccessService;

    @Mock
    private GuardianAccountService guardianAccountService;

    @InjectMocks
    private GuardianDeviceEnrollmentService enrollmentService;

    @Test
    void createReturnsSecureInvitationAndRevokesPreviousOnes() {
        Guardian guardian = createGuardian(true);

        GuardianDeviceEnrollment previous =
                new GuardianDeviceEnrollment();

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));
        when(guardianAccountService.ensureAccount(guardian))
                .thenReturn(createAccount(guardian, false));

        when(enrollmentRepository
                .findAllByGuardian_IdInAndUsedAtIsNullAndRevokedAtIsNull(
                        List.of(1L)
                ))
                .thenReturn(List.of(previous));

        when(enrollmentRepository.save(
                any(GuardianDeviceEnrollment.class)
        )).thenAnswer(invocation -> {
            GuardianDeviceEnrollment enrollment =
                    invocation.getArgument(0);

            enrollment.setId(100L);
            return enrollment;
        });

        CreateGuardianDeviceEnrollmentResponse response =
                enrollmentService.create(1L);

        ArgumentCaptor<GuardianDeviceEnrollment> captor =
                ArgumentCaptor.forClass(
                        GuardianDeviceEnrollment.class
                );

        verify(enrollmentRepository).save(captor.capture());
        verify(enrollmentRepository).saveAll(List.of(previous));
        verify(schoolAccessService).requireAccessToSchool(10L);

        GuardianDeviceEnrollment savedEnrollment =
                captor.getValue();

        assertEquals(1L, response.guardianId());
        assertEquals("Tutor de Prueba 3", response.guardianName());
        assertEquals("Escuela de Prueba 4", response.schoolName());
        assertEquals("TUT-1", response.username());

        assertTrue(
                response.enrollmentToken()
                        .matches("^[A-Za-z0-9_-]{43}$")
        );

        assertTrue(
                savedEnrollment.getTokenHash()
                        .matches("^[a-f0-9]{64}$")
        );

        assertNotEquals(
                response.enrollmentToken(),
                savedEnrollment.getTokenHash()
        );

        assertSame(guardian, savedEnrollment.getGuardian());
        assertNotNull(savedEnrollment.getExpiresAt());
        assertNotNull(previous.getRevokedAt());
    }

    @Test
    void createBatchGeneratesDistinctInvitationsForOneSchool() {
        Guardian first = createGuardian(true);
        Guardian second = createGuardian(true);
        second.setId(2L);
        second.setFullName("Tutor de Prueba 7");

        List<Long> guardianIds = List.of(1L, 2L);

        when(guardianRepository
                .findAllBySchool_IdAndIdInOrderByFullNameAsc(
                        10L,
                        guardianIds
                ))
                .thenReturn(List.of(first, second));
        when(guardianAccountService.ensureAccounts(List.of(first, second)))
                .thenReturn(List.of(
                        createAccount(first, false),
                        createAccount(second, false)
                ));
        when(enrollmentRepository
                .findAllByGuardian_IdInAndUsedAtIsNullAndRevokedAtIsNull(
                        guardianIds
                ))
                .thenReturn(List.of());

        GuardianInvitationBatchResponse response =
                enrollmentService.createBatch(
                        new CreateGuardianInvitationsRequest(
                                10L,
                                guardianIds
                        )
                );

        assertEquals(2, response.invitationsCreated());
        assertEquals(2, response.invitations().size());
        assertNotEquals(
                response.invitations().get(0).enrollmentToken(),
                response.invitations().get(1).enrollmentToken()
        );
        assertEquals(
                response.invitations().get(0).expiresAt(),
                response.invitations().get(1).expiresAt()
        );
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void createBatchRejectsGuardianOutsideSchoolBeforeWriting() {
        List<Long> guardianIds = List.of(1L, 2L);

        when(guardianRepository
                .findAllBySchool_IdAndIdInOrderByFullNameAsc(
                        10L,
                        guardianIds
                ))
                .thenReturn(List.of(createGuardian(true)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.createBatch(
                        new CreateGuardianInvitationsRequest(
                                10L,
                                guardianIds
                        )
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(enrollmentRepository, never())
                .save(any(GuardianDeviceEnrollment.class));
    }

    @Test
    void completeRegistersDeviceAndMarksInvitationAsUsed() {
        String enrollmentToken =
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO12";

        Guardian guardian = createGuardian(true);

        GuardianDeviceEnrollment enrollment =
                createEnrollment(
                        guardian,
                        enrollmentToken,
                        OffsetDateTime.now().plusMinutes(10)
                );

        CompleteGuardianDeviceEnrollmentRequest request =
                new CompleteGuardianDeviceEnrollmentRequest(
                        enrollmentToken,
                        "fcm-token-1",
                        "Teléfono de Tutor de Prueba 6Uno",
                        null
                );

        GuardianDeviceResponse expectedResponse =
                new GuardianDeviceResponse(
                        50L,
                        1L,
                        "Teléfono de Tutor de Prueba 6Uno",
                        true,
                        OffsetDateTime.now(),
                        null
                );

        when(enrollmentRepository.findByTokenHash(
                hashToken(enrollmentToken)
        )).thenReturn(Optional.of(enrollment));
        when(guardianAccountService.ensureAccount(guardian))
                .thenReturn(createAccount(guardian, true));

        when(guardianDeviceService.registerFromEnrollment(
                eq(guardian),
                any(RegisterGuardianDeviceRequest.class)
        )).thenReturn(expectedResponse);

        OffsetDateTime sessionExpiresAt =
                OffsetDateTime.now().plusDays(30);

        when(guardianSessionService.issue(
                guardian,
                50L,
                "Teléfono de Tutor de Prueba 6Uno"
        )).thenReturn(new IssuedGuardianSession(
                "guardian-session-token",
                sessionExpiresAt
        ));

        CompletedGuardianEnrollment completed =
                enrollmentService.complete(request);

        ArgumentCaptor<RegisterGuardianDeviceRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        RegisterGuardianDeviceRequest.class
                );

        verify(guardianDeviceService)
                .registerFromEnrollment(
                        eq(guardian),
                        requestCaptor.capture()
                );

        RegisterGuardianDeviceRequest registrationRequest =
                requestCaptor.getValue();

        assertEquals("fcm-token-1", registrationRequest.fcmToken());
        assertEquals(
                "Teléfono de Tutor de Prueba 6Uno",
                registrationRequest.deviceName()
        );

        assertSame(expectedResponse, completed.response().device());
        assertEquals(
                "guardian-session-token",
                completed.sessionToken()
        );
        assertEquals(
                sessionExpiresAt,
                completed.response().sessionExpiresAt()
        );
        assertNotNull(enrollment.getUsedAt());

        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void firstActivationCreatesPasswordWithoutRequiringNotifications() {
        String enrollmentToken = "first-activation-token";
        Guardian guardian = createGuardian(true);
        GuardianDeviceEnrollment enrollment = createEnrollment(
                guardian,
                enrollmentToken,
                OffsetDateTime.now().plusMinutes(10)
        );
        GuardianAccount pendingAccount = createAccount(guardian, false);
        GuardianAccount activatedAccount = createAccount(guardian, true);

        when(enrollmentRepository.findByTokenHash(hashToken(enrollmentToken)))
                .thenReturn(Optional.of(enrollment));
        when(guardianAccountService.ensureAccount(guardian))
                .thenReturn(pendingAccount);
        when(guardianAccountService.setPassword(
                pendingAccount,
                "segura-123"
        )).thenReturn(activatedAccount);
        when(guardianSessionService.issue(
                guardian,
                null,
                "Equipo Windows"
        )).thenReturn(new IssuedGuardianSession(
                "session-token",
                OffsetDateTime.now().plusDays(90)
        ));

        CompletedGuardianEnrollment completed = enrollmentService.complete(
                new CompleteGuardianDeviceEnrollmentRequest(
                        enrollmentToken,
                        null,
                        "Equipo Windows",
                        "segura-123"
                )
        );

        assertEquals("TUT-1", completed.response().username());
        assertEquals(false, completed.response().notificationsEnabled());
        assertEquals(null, completed.response().device());
        verifyNoInteractions(guardianDeviceService);
        verify(guardianAccountService).setPassword(
                pendingAccount,
                "segura-123"
        );
        verify(guardianSessionService).issue(
                guardian,
                null,
                "Equipo Windows"
        );
    }

    @Test
    void passwordResetChangesPasswordAndRevokesEveryPreviousAccess() {
        String enrollmentToken = "password-reset-token";
        Guardian guardian = createGuardian(true);
        GuardianDeviceEnrollment enrollment = createEnrollment(
                guardian,
                enrollmentToken,
                OffsetDateTime.now().plusMinutes(10)
        );
        enrollment.setPurpose(GuardianEnrollmentPurpose.PASSWORD_RESET);
        GuardianAccount account = createAccount(guardian, true);

        when(enrollmentRepository.findByTokenHash(hashToken(enrollmentToken)))
                .thenReturn(Optional.of(enrollment));
        when(guardianAccountService.ensureAccount(guardian))
                .thenReturn(account);
        when(guardianAccountService.setPassword(account, "nueva-123"))
                .thenReturn(account);
        when(guardianSessionService.issue(
                guardian,
                null,
                "Nuevo teléfono"
        )).thenReturn(new IssuedGuardianSession(
                "replacement-session",
                OffsetDateTime.now().plusDays(90)
        ));

        CompletedGuardianEnrollment completed = enrollmentService.complete(
                new CompleteGuardianDeviceEnrollmentRequest(
                        enrollmentToken,
                        null,
                        "Nuevo teléfono",
                        "nueva-123"
                )
        );

        verify(guardianSessionService).revokeAll(guardian.getId());
        verify(guardianDeviceService).deactivateAll(guardian.getId());
        verify(guardianAccountService).setPassword(account, "nueva-123");
        assertEquals("replacement-session", completed.sessionToken());
        assertNotNull(enrollment.getUsedAt());
    }

    @Test
    void completeRejectsExpiredInvitation() {
        String enrollmentToken =
                "expired-enrollment-token";

        GuardianDeviceEnrollment enrollment =
                createEnrollment(
                        createGuardian(true),
                        enrollmentToken,
                        OffsetDateTime.now().minusMinutes(1)
                );

        when(enrollmentRepository.findByTokenHash(
                hashToken(enrollmentToken)
        )).thenReturn(Optional.of(enrollment));

        CompleteGuardianDeviceEnrollmentRequest request =
                new CompleteGuardianDeviceEnrollmentRequest(
                        enrollmentToken,
                        "fcm-token-1",
                        "Teléfono",
                        null
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.complete(request)
        );

        assertEquals(
                HttpStatus.GONE,
                exception.getStatusCode()
        );

        verifyNoInteractions(guardianDeviceService);
        verify(enrollmentRepository, never())
                .save(any(GuardianDeviceEnrollment.class));
    }

    @Test
    void completeRejectsPreviouslyUsedInvitation() {
        String enrollmentToken =
                "used-enrollment-token";

        GuardianDeviceEnrollment enrollment =
                createEnrollment(
                        createGuardian(true),
                        enrollmentToken,
                        OffsetDateTime.now().plusMinutes(10)
                );

        enrollment.setUsedAt(OffsetDateTime.now());

        when(enrollmentRepository.findByTokenHash(
                hashToken(enrollmentToken)
        )).thenReturn(Optional.of(enrollment));

        CompleteGuardianDeviceEnrollmentRequest request =
                new CompleteGuardianDeviceEnrollmentRequest(
                        enrollmentToken,
                        "fcm-token-1",
                        "Teléfono",
                        null
                );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.complete(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(guardianDeviceService);
        verify(enrollmentRepository, never())
                .save(any(GuardianDeviceEnrollment.class));
    }

    private Guardian createGuardian(boolean active) {
        School school = new School();
        school.setId(10L);
        school.setName("Escuela de Prueba 4");
        school.setCode("ESC-TEST-3");

        Guardian guardian = new Guardian();
        guardian.setId(1L);
        guardian.setSchool(school);
        guardian.setFullName("Tutor de Prueba 3");
        guardian.setActive(active);

        return guardian;
    }

    private GuardianAccount createAccount(
            Guardian guardian,
            boolean activated
    ) {
        GuardianAccount account = new GuardianAccount();
        account.setId(200L + guardian.getId());
        account.setGuardian(guardian);
        account.setSchool(guardian.getSchool());
        account.setUsername("TUT-" + guardian.getId());
        if (activated) {
            account.setPasswordHash("encoded-password");
        }
        return account;
    }

    private GuardianDeviceEnrollment createEnrollment(
            Guardian guardian,
            String enrollmentToken,
            OffsetDateTime expiresAt
    ) {
        GuardianDeviceEnrollment enrollment =
                new GuardianDeviceEnrollment();

        enrollment.setId(100L);
        enrollment.setGuardian(guardian);
        enrollment.setTokenHash(hashToken(enrollmentToken));
        enrollment.setExpiresAt(expiresAt);

        return enrollment;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
