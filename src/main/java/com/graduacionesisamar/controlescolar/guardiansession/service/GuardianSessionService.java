package com.graduacionesisamar.controlescolar.guardiansession.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardiansession.entity.GuardianSession;
import com.graduacionesisamar.controlescolar.guardiansession.repository.GuardianSessionRepository;
import com.graduacionesisamar.controlescolar.guardiansession.security.GuardianPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Issues, validates and revokes opaque server-side guardian sessions.
 */
@Service
@Transactional
public class GuardianSessionService {

    private static final int TOKEN_BYTES = 32;
    private static final Duration LAST_USED_WRITE_INTERVAL =
            Duration.ofMinutes(5);

    private final GuardianSessionRepository guardianSessionRepository;
    private final Duration sessionDuration;
    private final SecureRandom secureRandom = new SecureRandom();

    public GuardianSessionService(
            GuardianSessionRepository guardianSessionRepository,
            @Value("${app.guardian-session.duration-days:90}")
            long sessionDurationDays
    ) {
        if (sessionDurationDays < 1 || sessionDurationDays > 365) {
            throw new IllegalArgumentException(
                    "Guardian session duration must be between 1 and 365 days"
            );
        }
        this.guardianSessionRepository = guardianSessionRepository;
        this.sessionDuration = Duration.ofDays(sessionDurationDays);
    }

    public IssuedGuardianSession issue(
            Guardian guardian,
            Long guardianDeviceId,
            String deviceName
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        String rawToken = generateToken();

        GuardianSession session = new GuardianSession();
        session.setGuardian(guardian);
        session.setGuardianDeviceId(guardianDeviceId);
        session.setTokenHash(hashToken(rawToken));
        session.setDeviceName(trimNullable(deviceName));
        session.setExpiresAt(now.plus(sessionDuration));
        session.setLastUsedAt(now);

        GuardianSession saved = guardianSessionRepository.save(session);

        return new IssuedGuardianSession(
                rawToken,
                saved.getExpiresAt()
        );
    }

    public Optional<GuardianPrincipal> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        GuardianSession session = guardianSessionRepository
                .findByTokenHash(hashToken(rawToken.trim()))
                .orElse(null);

        OffsetDateTime now = OffsetDateTime.now();

        if (!isActive(session, now)) {
            return Optional.empty();
        }

        if (session.getLastUsedAt() == null
                || session.getLastUsedAt()
                .isBefore(now.minus(LAST_USED_WRITE_INTERVAL))) {
            session.setLastUsedAt(now);
            guardianSessionRepository.save(session);
        }

        Guardian guardian = session.getGuardian();

        return Optional.of(new GuardianPrincipal(
                session.getId(),
                guardian.getId(),
                session.getGuardianDeviceId(),
                guardian.getSchool().getId(),
                guardian.getFullName(),
                guardian.getSchool().getName(),
                session.getExpiresAt()
        ));
    }

    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        guardianSessionRepository
                .findByTokenHash(hashToken(rawToken.trim()))
                .filter(session -> session.getRevokedAt() == null)
                .ifPresent(session -> {
                    session.setRevokedAt(OffsetDateTime.now());
                    guardianSessionRepository.save(session);
                });
    }

    public void attachDevice(
            Long sessionId,
            Long guardianId,
            Long guardianDeviceId
    ) {
        GuardianSession session = guardianSessionRepository
                .findByIdAndGuardian_Id(sessionId, guardianId)
                .orElseThrow(() -> new IllegalStateException(
                        "Guardian session not found"
                ));
        session.setGuardianDeviceId(guardianDeviceId);
        guardianSessionRepository.save(session);
    }

    public int revokeAll(Long guardianId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<GuardianSession> sessions = guardianSessionRepository
                .findAllByGuardian_IdAndRevokedAtIsNullOrderByCreatedAtDesc(
                        guardianId
                )
                .stream()
                .filter(session -> session.getExpiresAt().isAfter(now))
                .toList();
        sessions.forEach(session -> session.setRevokedAt(now));
        guardianSessionRepository.saveAll(sessions);
        return sessions.size();
    }

    public int revokeForDevice(Long guardianDeviceId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<GuardianSession> sessions = guardianSessionRepository
                .findAllByGuardianDeviceIdAndRevokedAtIsNull(
                        guardianDeviceId
                )
                .stream()
                .filter(session -> session.getExpiresAt().isAfter(now))
                .toList();
        sessions.forEach(session -> session.setRevokedAt(now));
        guardianSessionRepository.saveAll(sessions);
        return sessions.size();
    }

    private boolean isActive(
            GuardianSession session,
            OffsetDateTime now
    ) {
        return session != null
                && session.getRevokedAt() == null
                && session.getExpiresAt().isAfter(now)
                && Boolean.TRUE.equals(
                session.getGuardian().getActive()
        );
    }

    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
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
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private String trimNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
