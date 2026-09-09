package com.graduacionesisamar.controlescolar.guardianactivation.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardianaccount.repository.GuardianAccountRepository;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianAccessRevocationResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationPageResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationSelectionResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationStatusResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationStudentResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationSummaryResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianSessionAccessResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.RevokeGuardianAccessRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianDeviceEnrollment;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.repository.GuardianDeviceEnrollmentRepository;
import com.graduacionesisamar.controlescolar.guardiansession.entity.GuardianSession;
import com.graduacionesisamar.controlescolar.guardiansession.repository.GuardianSessionRepository;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import com.graduacionesisamar.controlescolar.studentguardian.entity.StudentGuardian;
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Administrative status and revocation operations for guardian access.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuardianActivationAdministrationService {

    private static final int MAX_MASS_SELECTION = 2_000;

    private final GuardianRepository guardianRepository;
    private final GuardianDeviceEnrollmentRepository enrollmentRepository;
    private final GuardianSessionRepository guardianSessionRepository;
    private final GuardianDeviceRepository guardianDeviceRepository;
    private final SchoolRepository schoolRepository;
    private final AcademicCycleRepository academicCycleRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SchoolAccessService schoolAccessService;
    private final GuardianAccountRepository guardianAccountRepository;

    @Transactional(readOnly = true)
    public GuardianActivationPageResponse findPage(
            Long schoolId,
            int page,
            int size,
            String search,
            String state,
            Long academicCycleId,
            Long schoolGroupId,
            String gradeName,
            String contact
    ) {
        String normalizedState = normalizeState(state);
        List<GuardianActivationStatusResponse> statuses = loadStatuses(
                schoolId,
                academicCycleId,
                schoolGroupId,
                gradeName,
                contact,
                search
        );
        List<GuardianActivationStatusResponse> filtered = statuses.stream()
                .filter(status -> matchesState(status, normalizedState))
                .toList();

        int fromIndex = (int) Math.min(
                (long) page * size,
                filtered.size()
        );
        int toIndex = Math.min(fromIndex + size, filtered.size());
        int totalPages = filtered.isEmpty()
                ? 0
                : (filtered.size() + size - 1) / size;

        return new GuardianActivationPageResponse(
                filtered.subList(fromIndex, toIndex),
                page,
                size,
                filtered.size(),
                totalPages,
                page == 0,
                page >= totalPages - 1,
                summarize(statuses)
        );
    }

    @Transactional(readOnly = true)
    public GuardianActivationSelectionResponse findSelection(
            Long schoolId,
            String search,
            String state,
            Long academicCycleId,
            Long schoolGroupId,
            String gradeName,
            String contact
    ) {
        String normalizedState = normalizeState(state);
        List<Long> ids = loadStatuses(
                schoolId,
                academicCycleId,
                schoolGroupId,
                gradeName,
                contact,
                search
        ).stream()
                .filter(status -> matchesState(status, normalizedState))
                .filter(GuardianActivationStatusResponse::guardianActive)
                .map(GuardianActivationStatusResponse::guardianId)
                .toList();

        if (ids.size() > MAX_MASS_SELECTION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Filter matches more than 2000 guardians"
            );
        }

        return new GuardianActivationSelectionResponse(ids, ids.size());
    }

    private List<GuardianActivationStatusResponse> loadStatuses(
            Long schoolId,
            Long academicCycleId,
            Long schoolGroupId,
            String gradeName,
            String contact,
            String search
    ) {
        schoolAccessService.requireAccessToSchool(schoolId);
        requireSchool(schoolId);
        requireAcademicCycle(schoolId, academicCycleId);

        String normalizedContact = normalizeContact(contact);
        List<Guardian> guardians = guardianRepository.findForActivation(
                schoolId,
                academicCycleId,
                schoolGroupId,
                normalize(gradeName),
                normalizedContact,
                normalize(search)
        );

        if (guardians.isEmpty()) {
            return List.of();
        }

        List<Long> guardianIds = guardians.stream()
                .map(Guardian::getId)
                .toList();
        Map<Long, List<GuardianDeviceEnrollment>> enrollmentByGuardian =
                enrollmentRepository
                        .findAllByGuardian_IdInOrderByCreatedAtDesc(
                                guardianIds
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                item -> item.getGuardian().getId()
                        ));
        Map<Long, List<GuardianSession>> sessionsByGuardian =
                guardianSessionRepository.findAllByGuardian_IdIn(guardianIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                item -> item.getGuardian().getId()
                        ));
        Map<Long, List<GuardianDevice>> devicesByGuardian =
                guardianDeviceRepository.findAllByGuardian_IdIn(guardianIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                item -> item.getGuardian().getId()
                        ));
        Map<Long, GuardianAccount> accountsByGuardian =
                guardianAccountRepository.findAllByGuardian_IdIn(guardianIds)
                        .stream()
                        .collect(Collectors.toMap(
                                item -> item.getGuardian().getId(),
                                item -> item
                        ));
        Map<Long, List<GuardianActivationStudentResponse>> studentsByGuardian =
                studentGuardianRepository.findForGuardianSummaries(
                                guardianIds,
                                academicCycleId
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                item -> item.getGuardian().getId(),
                                Collectors.mapping(
                                        this::toStudentSummary,
                                        Collectors.toList()
                                )
                        ));

        OffsetDateTime now = OffsetDateTime.now();
        return guardians.stream()
                .map(guardian -> toStatus(
                        guardian,
                        accountsByGuardian.get(guardian.getId()),
                        enrollmentByGuardian.getOrDefault(
                                guardian.getId(),
                                List.of()
                        ),
                        sessionsByGuardian.getOrDefault(
                                guardian.getId(),
                                List.of()
                        ),
                        devicesByGuardian.getOrDefault(
                                guardian.getId(),
                                List.of()
                        ),
                        studentsByGuardian.getOrDefault(
                                guardian.getId(),
                                List.of()
                        ),
                        now
                ))
                .toList();
    }

    private GuardianActivationSummaryResponse summarize(
            List<GuardianActivationStatusResponse> statuses
    ) {
        return new GuardianActivationSummaryResponse(
                statuses.size(),
                countState(statuses, "NOT_INVITED"),
                countState(statuses, "PENDING"),
                statuses.stream()
                        .filter(GuardianActivationStatusResponse::accountActivated)
                        .count(),
                statuses.stream()
                        .filter(GuardianActivationStatusResponse::guardianActive)
                        .filter(item -> !item.accountActivated())
                        .count(),
                statuses.stream()
                        .filter(item -> isBlank(item.phone()))
                        .filter(item -> isBlank(item.email()))
                        .count()
        );
    }

    private long countState(
            List<GuardianActivationStatusResponse> statuses,
            String state
    ) {
        return statuses.stream()
                .filter(item -> state.equals(item.activationState()))
                .count();
    }

    private boolean matchesState(
            GuardianActivationStatusResponse status,
            String state
    ) {
        return switch (state) {
            case "ALL" -> true;
            case "ACTIVE" -> status.accountActivated();
            case "PENDING" -> "PENDING".equals(status.activationState());
            case "NOT_INVITED" -> "NOT_INVITED".equals(
                    status.activationState()
            );
            case "EXPIRED_OR_REVOKED" -> Set.of(
                    "EXPIRED",
                    "REVOKED",
                    "ACCESS_REVOKED"
            ).contains(status.activationState());
            case "NOT_ACTIVE" -> status.guardianActive()
                    && !status.accountActivated();
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown activation state filter"
            );
        };
    }

    public GuardianAccessRevocationResponse revoke(
            RevokeGuardianAccessRequest request
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

        OffsetDateTime now = OffsetDateTime.now();

        List<GuardianDeviceEnrollment> invitations =
                enrollmentRepository
                        .findAllByGuardian_IdInAndUsedAtIsNullAndRevokedAtIsNull(
                                guardianIds
                        );
        invitations.forEach(item -> item.setRevokedAt(now));

        List<GuardianSession> sessions = guardianSessionRepository
                .findAllByGuardian_IdInAndRevokedAtIsNull(guardianIds)
                .stream()
                .filter(item -> item.getExpiresAt().isAfter(now))
                .toList();
        sessions.forEach(item -> item.setRevokedAt(now));

        List<GuardianDevice> devices = guardianDeviceRepository
                .findAllByGuardian_IdInAndActiveTrue(guardianIds);
        devices.forEach(item -> item.setActive(false));

        enrollmentRepository.saveAll(invitations);
        guardianSessionRepository.saveAll(sessions);
        guardianDeviceRepository.saveAll(devices);

        return new GuardianAccessRevocationResponse(
                guardianIds.size(),
                invitations.size(),
                sessions.size(),
                devices.size()
        );
    }

    private GuardianActivationStatusResponse toStatus(
            Guardian guardian,
            GuardianAccount account,
            List<GuardianDeviceEnrollment> enrollments,
            List<GuardianSession> sessions,
            List<GuardianDevice> devices,
            List<GuardianActivationStudentResponse> students,
            OffsetDateTime now
    ) {
        GuardianDeviceEnrollment latest = enrollments.stream()
                .max(Comparator.comparing(
                        GuardianDeviceEnrollment::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ))
                .orElse(null);

        int activeSessions = (int) sessions.stream()
                .filter(item -> item.getRevokedAt() == null)
                .filter(item -> item.getExpiresAt().isAfter(now))
                .count();
        int activeDevices = (int) devices.stream()
                .filter(GuardianDevice::isActive)
                .count();

        OffsetDateTime activatedAt = enrollments.stream()
                .map(GuardianDeviceEnrollment::getUsedAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new GuardianActivationStatusResponse(
                guardian.getId(),
                guardian.getExternalReference(),
                guardian.getFullName(),
                guardian.getPhone(),
                guardian.getEmail(),
                account == null ? null : account.getUsername(),
                account != null && account.isActivated(),
                Boolean.TRUE.equals(guardian.getActive()),
                resolveState(
                        guardian,
                        account,
                        latest,
                        activeSessions,
                        now
                ),
                latest == null ? null : latest.getCreatedAt(),
                latest == null ? null : latest.getExpiresAt(),
                activatedAt,
                activeDevices,
                activeSessions,
                List.copyOf(students)
        );
    }

    private GuardianActivationStudentResponse toStudentSummary(
            StudentGuardian link
    ) {
        var student = link.getStudent();
        var group = student.getSchoolGroup();
        return new GuardianActivationStudentResponse(
                student.getId(),
                student.getEnrollmentNumber(),
                student.getFirstName() + " " + student.getLastName(),
                group.getId(),
                group.getGradeName(),
                group.getGroupName()
        );
    }

    private String resolveState(
            Guardian guardian,
            GuardianAccount account,
            GuardianDeviceEnrollment latest,
            int activeSessions,
            OffsetDateTime now
    ) {
        if (!Boolean.TRUE.equals(guardian.getActive())) {
            return "INACTIVE";
        }

        if (activeSessions > 0) {
            return "ACTIVE";
        }

        if (account != null && account.isActivated()) {
            return "ACCOUNT_READY";
        }

        if (latest == null) {
            return "NOT_INVITED";
        }

        if (latest.getUsedAt() != null) {
            return "ACCESS_REVOKED";
        }

        if (latest.getRevokedAt() != null) {
            return "REVOKED";
        }

        if (!latest.getExpiresAt().isAfter(now)) {
            return "EXPIRED";
        }

        return "PENDING";
    }

    @Transactional(readOnly = true)
    public List<GuardianSessionAccessResponse> findActiveSessions(
            Long guardianId
    ) {
        Guardian guardian = findGuardianForAdministration(guardianId);
        schoolAccessService.requireAccessToSchool(
                guardian.getSchool().getId()
        );

        OffsetDateTime now = OffsetDateTime.now();
        Set<Long> activeDeviceIds = guardianDeviceRepository
                .findAllByGuardian_IdAndActiveTrueOrderByRegisteredAtDesc(
                        guardianId
                )
                .stream()
                .map(GuardianDevice::getId)
                .collect(Collectors.toSet());

        return guardianSessionRepository
                .findAllByGuardian_IdAndRevokedAtIsNullOrderByCreatedAtDesc(
                        guardianId
                )
                .stream()
                .filter(session -> session.getExpiresAt().isAfter(now))
                .map(session -> new GuardianSessionAccessResponse(
                        session.getId(),
                        session.getDeviceName(),
                        session.getGuardianDeviceId() != null
                                && activeDeviceIds.contains(
                                session.getGuardianDeviceId()
                        ),
                        session.getCreatedAt(),
                        session.getLastUsedAt(),
                        session.getExpiresAt()
                ))
                .toList();
    }

    public GuardianAccessRevocationResponse revokeSession(
            Long guardianId,
            Long sessionId
    ) {
        Guardian guardian = findGuardianForAdministration(guardianId);
        schoolAccessService.requireAccessToSchool(
                guardian.getSchool().getId()
        );

        GuardianSession selected = guardianSessionRepository
                .findByIdAndGuardian_Id(sessionId, guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian session not found"
                ));
        OffsetDateTime now = OffsetDateTime.now();
        Set<GuardianSession> sessionsToRevoke = new HashSet<>();
        if (selected.getRevokedAt() == null
                && selected.getExpiresAt().isAfter(now)) {
            sessionsToRevoke.add(selected);
        }

        int devicesDeactivated = 0;
        if (selected.getGuardianDeviceId() != null) {
            guardianSessionRepository
                    .findAllByGuardianDeviceIdAndRevokedAtIsNull(
                            selected.getGuardianDeviceId()
                    )
                    .stream()
                    .filter(session -> session.getExpiresAt().isAfter(now))
                    .forEach(sessionsToRevoke::add);

            GuardianDevice device = guardianDeviceRepository
                    .findByIdAndGuardian_Id(
                            selected.getGuardianDeviceId(),
                            guardianId
                    )
                    .orElse(null);
            if (device != null && device.isActive()) {
                device.setActive(false);
                guardianDeviceRepository.save(device);
                devicesDeactivated = 1;
            }
        }

        sessionsToRevoke.forEach(session -> session.setRevokedAt(now));
        guardianSessionRepository.saveAll(sessionsToRevoke);

        return new GuardianAccessRevocationResponse(
                1,
                0,
                sessionsToRevoke.size(),
                devicesDeactivated
        );
    }

    private Guardian findGuardianForAdministration(Long guardianId) {
        return guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian not found"
                ));
    }

    private List<Long> distinctIds(List<Long> guardianIds) {
        Set<Long> distinct = new HashSet<>(guardianIds);

        if (distinct.size() != guardianIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Guardian ids must not be repeated"
            );
        }

        return List.copyOf(guardianIds);
    }

    private void requireSchool(Long schoolId) {
        if (!schoolRepository.existsById(schoolId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "School not found"
            );
        }
    }

    private void requireAcademicCycle(
            Long schoolId,
            Long academicCycleId
    ) {
        if (academicCycleId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Academic cycle is required"
            );
        }
        AcademicCycle cycle = academicCycleRepository.findById(academicCycleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Academic cycle not found"
                ));
        if (!cycle.getSchool().getId().equals(schoolId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Academic cycle not found in this school"
            );
        }
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeContact(String contact) {
        String normalized = contact == null
                ? "ALL"
                : contact.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "AVAILABLE", "MISSING").contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown contact filter"
            );
        }
        return normalized;
    }

    private String normalizeState(String state) {
        String normalized = state == null
                ? "NOT_ACTIVE"
                : state.trim().toUpperCase(Locale.ROOT);
        if (!Set.of(
                "ALL",
                "ACTIVE",
                "PENDING",
                "NOT_INVITED",
                "EXPIRED_OR_REVOKED",
                "NOT_ACTIVE"
        ).contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown activation state filter"
            );
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
