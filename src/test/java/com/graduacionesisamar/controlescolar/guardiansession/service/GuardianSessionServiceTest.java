package com.graduacionesisamar.controlescolar.guardiansession.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardiansession.entity.GuardianSession;
import com.graduacionesisamar.controlescolar.guardiansession.repository.GuardianSessionRepository;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import com.graduacionesisamar.controlescolar.school.entity.School;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianSessionServiceTest {

    @Mock
    private GuardianSessionRepository guardianSessionRepository;

    private GuardianSessionService guardianSessionService;

    @BeforeEach
    void setUp() {
        guardianSessionService = new GuardianSessionService(
                guardianSessionRepository,
                90
        );
    }

    @Test
    void issueStoresOnlyAHashAndReturnsOpaqueToken() {
        Guardian guardian = createGuardian(true);

        when(guardianSessionRepository.save(
                any(GuardianSession.class)
        )).thenAnswer(invocation -> {
            GuardianSession session = invocation.getArgument(0);
            session.setId(100L);
            return session;
        });

        IssuedGuardianSession issued = guardianSessionService.issue(
                guardian,
                55L,
                " Teléfono principal "
        );

        ArgumentCaptor<GuardianSession> captor =
                ArgumentCaptor.forClass(GuardianSession.class);
        verify(guardianSessionRepository).save(captor.capture());

        GuardianSession stored = captor.getValue();

        assertTrue(issued.token().matches("^[A-Za-z0-9_-]{43}$"));
        assertTrue(stored.getTokenHash().matches("^[a-f0-9]{64}$"));
        assertNotEquals(issued.token(), stored.getTokenHash());
        assertEquals(55L, stored.getGuardianDeviceId());
        assertEquals("Teléfono principal", stored.getDeviceName());
        assertTrue(issued.expiresAt().isAfter(
                OffsetDateTime.now().plusDays(89)
        ));
    }

    @Test
    void authenticateBuildsGuardianPrincipalFromValidSession() {
        String rawToken = "opaque-guardian-session";
        GuardianSession session = createSession(
                createGuardian(true),
                OffsetDateTime.now().plusDays(5)
        );
        session.setLastUsedAt(OffsetDateTime.now());

        when(guardianSessionRepository.findByTokenHash(
                hashToken(rawToken)
        )).thenReturn(Optional.of(session));

        Optional<GuardianPrincipal> result =
                guardianSessionService.authenticate(rawToken);

        assertTrue(result.isPresent());
        assertEquals(1L, result.orElseThrow().guardianId());
        assertEquals(10L, result.orElseThrow().schoolId());
        assertEquals(
                "Tutor de Prueba 3",
                result.orElseThrow().guardianName()
        );
    }

    @Test
    void authenticateRejectsRevokedExpiredAndInactiveSessions() {
        GuardianSession revoked = createSession(
                createGuardian(true),
                OffsetDateTime.now().plusDays(1)
        );
        revoked.setRevokedAt(OffsetDateTime.now());

        GuardianSession expired = createSession(
                createGuardian(true),
                OffsetDateTime.now().minusMinutes(1)
        );

        GuardianSession inactive = createSession(
                createGuardian(false),
                OffsetDateTime.now().plusDays(1)
        );

        when(guardianSessionRepository.findByTokenHash(
                hashToken("revoked")
        )).thenReturn(Optional.of(revoked));
        when(guardianSessionRepository.findByTokenHash(
                hashToken("expired")
        )).thenReturn(Optional.of(expired));
        when(guardianSessionRepository.findByTokenHash(
                hashToken("inactive")
        )).thenReturn(Optional.of(inactive));

        assertFalse(guardianSessionService
                .authenticate("revoked").isPresent());
        assertFalse(guardianSessionService
                .authenticate("expired").isPresent());
        assertFalse(guardianSessionService
                .authenticate("inactive").isPresent());
    }

    @Test
    void revokeMarksOnlyKnownActiveSession() {
        String rawToken = "session-to-revoke";
        GuardianSession session = createSession(
                createGuardian(true),
                OffsetDateTime.now().plusDays(1)
        );

        when(guardianSessionRepository.findByTokenHash(
                hashToken(rawToken)
        )).thenReturn(Optional.of(session));

        guardianSessionService.revoke(rawToken);

        assertTrue(session.getRevokedAt() != null);
        verify(guardianSessionRepository).save(session);
    }

    private Guardian createGuardian(boolean active) {
        School school = new School();
        school.setId(10L);
        school.setName("Escuela de Prueba 4");

        Guardian guardian = new Guardian();
        guardian.setId(1L);
        guardian.setSchool(school);
        guardian.setFullName("Tutor de Prueba 3");
        guardian.setActive(active);
        return guardian;
    }

    private GuardianSession createSession(
            Guardian guardian,
            OffsetDateTime expiresAt
    ) {
        GuardianSession session = new GuardianSession();
        session.setId(100L);
        session.setGuardian(guardian);
        session.setGuardianDeviceId(55L);
        session.setTokenHash("hash");
        session.setExpiresAt(expiresAt);
        session.setLastUsedAt(OffsetDateTime.now());
        return session;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            ));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
