package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.CreateGuardianInvitationsRequest;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianInvitationBatchResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentRequest;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CreateGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianDeviceEnrollment;
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

    @InjectMocks
    private GuardianDeviceEnrollmentService enrollmentService;

    @Test
    void createReturnsSecureInvitationAndRevokesPreviousOnes() {
        Guardian guardian = createGuardian(true);

        GuardianDeviceEnrollment previous =
                new GuardianDeviceEnrollment();

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));

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
        assertEquals("Tutor de prueba", response.guardianName());
        assertEquals("Escuela de prueba", response.schoolName());

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
        second.setFullName("Segundo tutor");

        List<Long> guardianIds = List.of(1L, 2L);

        when(guardianRepository
                .findAllBySchool_IdAndIdInOrderByFullNameAsc(
                        10L,
                        guardianIds
                ))
                .thenReturn(List.of(first, second));
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
                        "Teléfono de Samuel"
                );

        GuardianDeviceResponse expectedResponse =
                new GuardianDeviceResponse(
                        50L,
                        1L,
                        "Teléfono de Samuel",
                        true,
                        OffsetDateTime.now(),
                        null
                );

        when(enrollmentRepository.findByTokenHash(
                hashToken(enrollmentToken)
        )).thenReturn(Optional.of(enrollment));

        when(guardianDeviceService.registerFromEnrollment(
                eq(guardian),
                any(RegisterGuardianDeviceRequest.class)
        )).thenReturn(expectedResponse);

        OffsetDateTime sessionExpiresAt =
                OffsetDateTime.now().plusDays(30);

        when(guardianSessionService.issue(
                guardian,
                50L,
                "Teléfono de Samuel"
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
                "Teléfono de Samuel",
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
                        "Teléfono"
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
                        "Teléfono"
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
        school.setName("Escuela de prueba");

        Guardian guardian = new Guardian();
        guardian.setId(1L);
        guardian.setSchool(school);
        guardian.setFullName("Tutor de prueba");
        guardian.setActive(active);

        return guardian;
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
