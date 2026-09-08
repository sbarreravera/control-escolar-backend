package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentRequest;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CreateGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianDeviceEnrollment;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.repository.GuardianDeviceEnrollmentRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Creates and completes temporary invitations used to register
 * guardian notification devices.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuardianDeviceEnrollmentService {

    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRATION_MINUTES = 15;

    private final GuardianDeviceEnrollmentRepository enrollmentRepository;
    private final GuardianRepository guardianRepository;
    private final GuardianDeviceService guardianDeviceService;
    private final SchoolAccessService schoolAccessService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates a new one-time enrollment invitation.
     */
    public CreateGuardianDeviceEnrollmentResponse create(
            Long guardianId
    ) {
        Guardian guardian = findGuardian(guardianId);

        schoolAccessService.requireAccessToSchool(
                guardian.getSchool().getId()
        );

        if (!Boolean.TRUE.equals(guardian.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inactive guardian cannot receive invitations"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        revokePreviousInvitations(guardianId, now);

        String enrollmentToken = generateToken();

        GuardianDeviceEnrollment enrollment =
                new GuardianDeviceEnrollment();

        enrollment.setGuardian(guardian);
        enrollment.setTokenHash(hashToken(enrollmentToken));
        enrollment.setExpiresAt(
                now.plusMinutes(EXPIRATION_MINUTES)
        );

        GuardianDeviceEnrollment savedEnrollment =
                enrollmentRepository.save(enrollment);

        return new CreateGuardianDeviceEnrollmentResponse(
                guardian.getId(),
                guardian.getFullName(),
                guardian.getSchool().getName(),
                enrollmentToken,
                savedEnrollment.getExpiresAt()
        );
    }

    /**
     * Validates a one-time invitation and registers
     * the guardian's device.
     */
    public GuardianDeviceResponse complete(
            CompleteGuardianDeviceEnrollmentRequest request
    ) {
        String enrollmentToken =
                request.enrollmentToken().trim();

        GuardianDeviceEnrollment enrollment =
                enrollmentRepository
                        .findByTokenHash(
                                hashToken(enrollmentToken)
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Enrollment invitation not found"
                                )
                        );

        OffsetDateTime now = OffsetDateTime.now();

        validateEnrollment(enrollment, now);

        GuardianDeviceResponse device =
                guardianDeviceService.registerFromEnrollment(
                        enrollment.getGuardian(),
                        new RegisterGuardianDeviceRequest(
                                request.fcmToken(),
                                request.deviceName()
                        )
                );

        enrollment.setUsedAt(now);
        enrollmentRepository.save(enrollment);

        return device;
    }

    private void revokePreviousInvitations(
            Long guardianId,
            OffsetDateTime revokedAt
    ) {
        List<GuardianDeviceEnrollment> invitations =
                enrollmentRepository
                        .findAllByGuardian_IdAndUsedAtIsNullAndRevokedAtIsNull(
                                guardianId
                        );

        invitations.forEach(
                invitation ->
                        invitation.setRevokedAt(revokedAt)
        );

        enrollmentRepository.saveAll(invitations);
    }

    private void validateEnrollment(
            GuardianDeviceEnrollment enrollment,
            OffsetDateTime now
    ) {
        if (enrollment.getUsedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Enrollment invitation was already used"
            );
        }

        if (enrollment.getRevokedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Enrollment invitation was revoked"
            );
        }

        if (!enrollment.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Enrollment invitation has expired"
            );
        }

        if (!Boolean.TRUE.equals(
                enrollment.getGuardian().getActive()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Guardian is inactive"
            );
        }
    }

    private Guardian findGuardian(Long guardianId) {
        return guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian not found"
                ));
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
}