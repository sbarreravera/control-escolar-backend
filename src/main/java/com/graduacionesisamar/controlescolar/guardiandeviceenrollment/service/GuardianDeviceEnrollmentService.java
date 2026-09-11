package com.graduacionesisamar.controlescolar.guardiandeviceenrollment.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardianaccount.service.GuardianAccountService;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.CreateGuardianInvitationsRequest;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianInvitationBatchResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianInvitationResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.GuardianDeviceResponse;
import com.graduacionesisamar.controlescolar.guardiandevice.dto.RegisterGuardianDeviceRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentRequest;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CompleteGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.CreateGuardianDeviceEnrollmentResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.dto.GuardianInvitationStatusResponse;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianDeviceEnrollment;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianEnrollmentPurpose;
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
    private final GuardianAccountService guardianAccountService;

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
        GuardianAccount account = guardianAccountService.ensureAccount(
                guardian
        );

        UUID batchId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(INVITATION_DURATION);

        revokePreviousInvitations(List.of(guardianId), now);

        GuardianInvitationResponse invitation =
                createInvitation(
                        guardian,
                        account,
                        batchId,
                        expiresAt,
                        GuardianEnrollmentPurpose.ACTIVATION
                );

        return new CreateGuardianDeviceEnrollmentResponse(
                invitation.guardianId(),
                invitation.guardianName(),
                invitation.schoolName(),
                invitation.schoolCode(),
                invitation.username(),
                invitation.purpose(),
                invitation.enrollmentToken(),
                invitation.expiresAt()
        );
    }

    /**
     * Creates invitations atomically for up to 2,000 guardians in one school.
     */
    public GuardianInvitationBatchResponse createBatch(
            CreateGuardianInvitationsRequest request
    ) {
        return createBatch(request, GuardianEnrollmentPurpose.ACTIVATION);
    }

    public GuardianInvitationBatchResponse createPasswordResetBatch(
            CreateGuardianInvitationsRequest request
    ) {
        return createBatch(request, GuardianEnrollmentPurpose.PASSWORD_RESET);
    }

    private GuardianInvitationBatchResponse createBatch(
            CreateGuardianInvitationsRequest request,
            GuardianEnrollmentPurpose purpose
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
        var accountsByGuardian = guardianAccountService
                .ensureAccounts(guardians)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        account -> account.getGuardian().getId(),
                        account -> account
                ));

        UUID batchId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plus(INVITATION_DURATION);

        revokePreviousInvitations(guardianIds, now);

        List<GuardianInvitationResponse> invitations =
                new ArrayList<>(guardians.size());

        for (Guardian guardian : guardians) {
            invitations.add(createInvitation(
                    guardian,
                    accountsByGuardian.get(guardian.getId()),
                    batchId,
                    expiresAt,
                    purpose
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
     * Consumes an invitation, configures credentials when required,
     * optionally registers FCM and issues an opaque session.
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
        GuardianAccount account = guardianAccountService.ensureAccount(
                guardian
        );

        boolean passwordRequired = !account.isActivated()
                || enrollment.getPurpose()
                == GuardianEnrollmentPurpose.PASSWORD_RESET;
        if (passwordRequired) {
            requirePassword(request.password());
        }

        if (enrollment.getPurpose()
                == GuardianEnrollmentPurpose.PASSWORD_RESET) {
            guardianSessionService.revokeAll(guardian.getId());
            guardianDeviceService.deactivateAll(guardian.getId());
        }

        if (passwordRequired) {
            account = guardianAccountService.setPassword(
                    account,
                    request.password()
            );
        }

        GuardianDeviceResponse device = null;
        if (request.fcmToken() != null
                && !request.fcmToken().isBlank()) {
            device = guardianDeviceService.registerFromEnrollment(
                        guardian,
                        new RegisterGuardianDeviceRequest(
                                request.fcmToken().trim(),
                                request.deviceName()
                        )
                );
        }

        IssuedGuardianSession session = guardianSessionService.issue(
                guardian,
                device == null ? null : device.id(),
                request.deviceName()
        );

        enrollment.setUsedAt(now);
        enrollmentRepository.save(enrollment);

        CompleteGuardianDeviceEnrollmentResponse response =
                new CompleteGuardianDeviceEnrollmentResponse(
                        guardian.getId(),
                        guardian.getFullName(),
                        guardian.getSchool().getName(),
                        guardian.getSchool().getCode(),
                        account.getUsername(),
                        device,
                        device != null,
                        session.expiresAt()
                );

        return new CompletedGuardianEnrollment(
                response,
                session.token()
        );
    }

    private GuardianInvitationResponse createInvitation(
            Guardian guardian,
            GuardianAccount account,
            UUID batchId,
            OffsetDateTime expiresAt,
            GuardianEnrollmentPurpose purpose
    ) {
        String enrollmentToken = generateToken();

        GuardianDeviceEnrollment enrollment =
                new GuardianDeviceEnrollment();
        enrollment.setGuardian(guardian);
        enrollment.setBatchId(batchId);
        enrollment.setPurpose(purpose);
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
                guardian.getSchool().getCode(),
                account.getUsername(),
                purpose,
                enrollmentToken,
                expiresAt
        );
    }

    @Transactional(readOnly = true)
    public GuardianInvitationStatusResponse findStatus(String token) {
        String normalizedToken = token == null ? "" : token.trim();
        if (normalizedToken.isBlank() || normalizedToken.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Enrollment invitation not found"
            );
        }

        GuardianDeviceEnrollment enrollment = enrollmentRepository
                .findForStatusByTokenHash(hashToken(normalizedToken))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Enrollment invitation not found"
                ));
        GuardianAccount account = guardianAccountService.findByGuardianId(
                enrollment.getGuardian().getId()
        );

        return new GuardianInvitationStatusResponse(
                resolveStatus(enrollment, OffsetDateTime.now()),
                enrollment.getPurpose(),
                enrollment.getGuardian().getId(),
                enrollment.getGuardian().getFullName(),
                enrollment.getGuardian().getSchool().getName(),
                enrollment.getGuardian().getSchool().getCode(),
                account.getUsername(),
                account.isActivated(),
                enrollment.getExpiresAt()
        );
    }

    private String resolveStatus(
            GuardianDeviceEnrollment enrollment,
            OffsetDateTime now
    ) {
        if (enrollment.getUsedAt() != null) {
            return "USED";
        }
        if (enrollment.getRevokedAt() != null) {
            return "REVOKED";
        }
        if (!enrollment.getExpiresAt().isAfter(now)) {
            return "EXPIRED";
        }
        return "VALID";
    }

    private void requirePassword(String password) {
        if (password == null
                || password.isBlank()
                || password.length() < 8
                || password.length() > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A password between 8 and 72 characters is required"
            );
        }
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
