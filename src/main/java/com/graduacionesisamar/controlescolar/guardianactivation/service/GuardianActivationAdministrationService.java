package com.graduacionesisamar.controlescolar.guardianactivation.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianAccessRevocationResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationPageResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.GuardianActivationStatusResponse;
import com.graduacionesisamar.controlescolar.guardianactivation.dto.RevokeGuardianAccessRequest;
import com.graduacionesisamar.controlescolar.guardiandevice.entity.GuardianDevice;
import com.graduacionesisamar.controlescolar.guardiandevice.repository.GuardianDeviceRepository;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.entity.GuardianDeviceEnrollment;
import com.graduacionesisamar.controlescolar.guardiandeviceenrollment.repository.GuardianDeviceEnrollmentRepository;
import com.graduacionesisamar.controlescolar.guardiansession.entity.GuardianSession;
import com.graduacionesisamar.controlescolar.guardiansession.repository.GuardianSessionRepository;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
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

    private final GuardianRepository guardianRepository;
    private final GuardianDeviceEnrollmentRepository enrollmentRepository;
    private final GuardianSessionRepository guardianSessionRepository;
    private final GuardianDeviceRepository guardianDeviceRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolAccessService schoolAccessService;

    @Transactional(readOnly = true)
    public GuardianActivationPageResponse findPage(
            Long schoolId,
            int page,
            int size,
            String search,
            String state
    ) {
        schoolAccessService.requireAccessToSchool(schoolId);
        requireSchool(schoolId);

        List<Guardian> guardians = guardianRepository
                .findAllBySchool_IdOrderByFullNameAsc(schoolId);
        List<GuardianDeviceEnrollment> enrollments =
                enrollmentRepository
                        .findAllByGuardian_School_IdOrderByCreatedAtDesc(
                                schoolId
                        );
        List<GuardianSession> sessions = guardianSessionRepository
                .findAllByGuardian_School_Id(schoolId);
        List<GuardianDevice> devices = guardianDeviceRepository
                .findAllByGuardian_School_Id(schoolId);

        Map<Long, List<GuardianDeviceEnrollment>> enrollmentByGuardian =
                enrollments.stream().collect(Collectors.groupingBy(
                        item -> item.getGuardian().getId()
                ));
        Map<Long, List<GuardianSession>> sessionsByGuardian =
                sessions.stream().collect(Collectors.groupingBy(
                        item -> item.getGuardian().getId()
                ));
        Map<Long, List<GuardianDevice>> devicesByGuardian =
                devices.stream().collect(Collectors.groupingBy(
                        item -> item.getGuardian().getId()
                ));

        OffsetDateTime now = OffsetDateTime.now();

        List<GuardianActivationStatusResponse> filtered = guardians.stream()
                .map(guardian -> toStatus(
                        guardian,
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
                        now
                ))
                .filter(status -> matchesSearch(status, search))
                .filter(status -> matchesState(status, state))
                .toList();

        int fromIndex = Math.min(page * size, filtered.size());
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
                page >= totalPages - 1
        );
    }

    private boolean matchesSearch(
            GuardianActivationStatusResponse status,
            String search
    ) {
        String normalized = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return true;
        }
        return List.of(
                status.guardianName(),
                status.externalReference() == null ? "" : status.externalReference(),
                status.phone() == null ? "" : status.phone(),
                status.email() == null ? "" : status.email()
        ).stream().anyMatch(value -> value.toLowerCase(Locale.ROOT)
                .contains(normalized));
    }

    private boolean matchesState(
            GuardianActivationStatusResponse status,
            String state
    ) {
        String normalized = state == null
                ? "NOT_ACTIVE"
                : state.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL" -> true;
            case "ACTIVE" -> "ACTIVE".equals(status.activationState());
            case "PENDING" -> "PENDING".equals(status.activationState());
            case "NOT_ACTIVE" -> status.guardianActive()
                    && !"ACTIVE".equals(status.activationState());
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
            List<GuardianDeviceEnrollment> enrollments,
            List<GuardianSession> sessions,
            List<GuardianDevice> devices,
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
                Boolean.TRUE.equals(guardian.getActive()),
                resolveState(
                        guardian,
                        latest,
                        activeSessions,
                        now
                ),
                latest == null ? null : latest.getCreatedAt(),
                latest == null ? null : latest.getExpiresAt(),
                activatedAt,
                activeDevices,
                activeSessions
        );
    }

    private String resolveState(
            Guardian guardian,
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
}
