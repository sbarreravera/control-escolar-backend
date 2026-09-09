package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.CreateGuardianInvitationsRequest;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianInvitationBatchResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianInvitationResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentRequest;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CreateGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianDeviceEnrollment;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.repository.GuardianDeviceEnrollmentRepository;
import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
import com.graduacionesisamar.controlescolar.guardiansession.service.IssuedGuardianSession;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Creates and completes one-time guardian activation invitations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuardianDeviceEnrollmentService {

    private static final int TOKEN_BYTES = 32;
    private static final Duration INVITATION_DURATION =
            Duration.ofDays(7);

    private final GuardianDeviceEnrollmentRepository enrollmentRepository;
    private final GuardianRepository guardianRepository;
    private final GuardianDeviceService guardianDeviceService;
    private final GuardianSessionService guardianSessionService;
    private final SchoolAccessService schoolAccessService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates a new one-time invitation for one guardian.
     */
    public CreateGuardianDeviceEnrollmentResponse create(
            Long guardianId
    ) {
        Guardian guardian = findGuardian(guardianId);

        schoolAccessService.requireAccessToSchool(
                guardian.getSchool().getId()
        );

        validateActive(guardian);

        UUID batchId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(INVITATION_DURATION);

        revokePreviousInvitations(List.of(guardianId), now);

        GuardianInvitationResponse invitation =
                createInvitation(guardian, batchId, expiresAt);

        return new CreateGuardianDeviceEnrollmentResponse(
                invitation.guardianId(),
                invitation.guardianName(),
                invitation.schoolName(),
                invitation.enrollmentToken(),
                invitation.expiresAt()
        );
    }

    /**
     * Creates invitations atomically for up to 500 guardians in one school.
     */
    public GuardianInvitationBatchResponse createBatch(
            CreateGuardianInvitationsRequest request
    ) {
        schoolAccessService.requireAccessToSchool(request.schoolId());

        List<Long> guardianIds = distinctIds(request.guardianIds());
        List<Guardian> guardians = guardianRepository
                .findAllBySchool_IdAndIdInOrderByFullNameAsc(
                        request.schoolId(),
                        guardianIds
                );

        if (guardians.size() != guardianIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "One or more guardians were not found in this school"
            );
        }

        guardians.forEach(this::validateActive);

        UUID batchId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(INVITATION_DURATION);

        revokePreviousInvitations(guardianIds, now);

        List<GuardianInvitationResponse> invitations =
                new ArrayList<>(guardians.size());

        for (Guardian guardian : guardians) {
            invitations.add(createInvitation(
                    guardian,
                    batchId,
                    expiresAt
            ));
        }

        return new GuardianInvitationBatchResponse(
                batchId,
                expiresAt,
                invitations.size(),
                List.copyOf(invitations)
        );
    }

    /**
     * Consumes an invitation, registers FCM and issues an opaque session.
     */
    public CompletedGuardianEnrollment complete(
            CompleteGuardianDeviceEnrollmentRequest request
    ) {
        String enrollmentToken = request.enrollmentToken().trim();

        GuardianDeviceEnrollment enrollment = enrollmentRepository
                .findByTokenHash(hashToken(enrollmentToken))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Enrollment invitation not found"
                ));

        OffsetDateTime now = OffsetDateTime.now();
        validateEnrollment(enrollment, now);

        Guardian guardian = enrollment.getGuardian();

        GuardianDeviceResponse device =
                guardianDeviceService.registerFromEnrollment(
                        guardian,
                        new RegisterGuardianDeviceRequest(
                                request.fcmToken(),
                                request.deviceName()
                        )
                );

        IssuedGuardianSession session = guardianSessionService.issue(
                guardian,
                device.id(),
                request.deviceName()
        );

        enrollment.setUsedAt(now);
        enrollmentRepository.save(enrollment);

        CompleteGuardianDeviceEnrollmentResponse response =
                new CompleteGuardianDeviceEnrollmentResponse(
                        guardian.getId(),
                        guardian.getFullName(),
                        guardian.getSchool().getName(),
                        device,
                        session.expiresAt()
                );

        return new CompletedGuardianEnrollment(
                response,
                session.token()
        );
    }

    private GuardianInvitationResponse createInvitation(
            Guardian guardian,
            UUID batchId,
            OffsetDateTime expiresAt
    ) {
        String enrollmentToken = generateToken();

        GuardianDeviceEnrollment enrollment =
                new GuardianDeviceEnrollment();
        enrollment.setGuardian(guardian);
        enrollment.setBatchId(batchId);
        enrollment.setTokenHash(hashToken(enrollmentToken));
        enrollment.setExpiresAt(expiresAt);

        enrollmentRepository.save(enrollment);

        return new GuardianInvitationResponse(
                guardian.getId(),
                guardian.getExternalReference(),
                guardian.getFullName(),
                guardian.getPhone(),
                guardian.getEmail(),
                guardian.getSchool().getName(),
                enrollmentToken,
                expiresAt
        );
    }

    private void revokePreviousInvitations(
            List<Long> guardianIds,
            OffsetDateTime revokedAt
    ) {
        List<GuardianDeviceEnrollment> invitations =
                enrollmentRepository
                        .findAllByGuardian_IdInAndUsedAtIsNullAndRevokedAtIsNull(
                                guardianIds
                        );

        invitations.forEach(
                invitation -> invitation.setRevokedAt(revokedAt)
        );

        enrollmentRepository.saveAll(invitations);
    }

    private List<Long> distinctIds(List<Long> guardianIds) {
        Set<Long> distinctIds = new HashSet<>(guardianIds);

        if (distinctIds.size() != guardianIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Guardian ids must not be repeated"
            );
        }

        return List.copyOf(guardianIds);
    }

    private void validateActive(Guardian guardian) {
        if (!Boolean.TRUE.equals(guardian.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inactive guardian cannot receive invitations"
            );
        }
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

        validateActive(enrollment.getGuardian());
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
