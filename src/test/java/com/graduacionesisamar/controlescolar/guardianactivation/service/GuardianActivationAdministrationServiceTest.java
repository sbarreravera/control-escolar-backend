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
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianActivationAdministrationServiceTest {

    @Mock
    private GuardianRepository guardianRepository;
    @Mock
    private GuardianDeviceEnrollmentRepository enrollmentRepository;
    @Mock
    private GuardianSessionRepository guardianSessionRepository;
    @Mock
    private GuardianDeviceRepository guardianDeviceRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private SchoolAccessService schoolAccessService;

    @InjectMocks
    private GuardianActivationAdministrationService service;

    @Test
    void findAllReportsActiveSessionAndDeviceCounts() {
        Guardian guardian = createGuardian(1L);
        GuardianDeviceEnrollment enrollment = new GuardianDeviceEnrollment();
        enrollment.setGuardian(guardian);
        enrollment.setCreatedAt(OffsetDateTime.now().minusDays(1));
        enrollment.setExpiresAt(OffsetDateTime.now().plusDays(6));
        enrollment.setUsedAt(OffsetDateTime.now().minusHours(2));

        GuardianSession session = new GuardianSession();
        session.setGuardian(guardian);
        session.setExpiresAt(OffsetDateTime.now().plusDays(10));

        GuardianDevice device = new GuardianDevice();
        device.setGuardian(guardian);
        device.setActive(true);

        when(schoolRepository.existsById(10L)).thenReturn(true);
        when(guardianRepository.findAllBySchool_IdOrderByFullNameAsc(10L))
                .thenReturn(List.of(guardian));
        when(enrollmentRepository
                .findAllByGuardian_School_IdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(enrollment));
        when(guardianSessionRepository
                .findAllByGuardian_School_Id(10L))
                .thenReturn(List.of(session));
        when(guardianDeviceRepository
                .findAllByGuardian_School_Id(10L))
                .thenReturn(List.of(device));

        GuardianActivationPageResponse page = service.findPage(
                10L, 0, 25, "", "ALL"
        );
        List<GuardianActivationStatusResponse> statuses = page.content();

        assertEquals(1, statuses.size());
        assertEquals(1, page.totalElements());
        assertEquals("ACTIVE", statuses.getFirst().activationState());
        assertEquals(1, statuses.getFirst().activeSessions());
        assertEquals(1, statuses.getFirst().activeDevices());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void findPageFiltersBeforeApplyingPagination() {
        Guardian first = createGuardian(1L);
        first.setFullName("Ana López");
        Guardian second = createGuardian(2L);
        second.setFullName("Bruno Pérez");

        when(schoolRepository.existsById(10L)).thenReturn(true);
        when(guardianRepository.findAllBySchool_IdOrderByFullNameAsc(10L))
                .thenReturn(List.of(first, second));
        when(enrollmentRepository
                .findAllByGuardian_School_IdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of());
        when(guardianSessionRepository
                .findAllByGuardian_School_Id(10L))
                .thenReturn(List.of());
        when(guardianDeviceRepository
                .findAllByGuardian_School_Id(10L))
                .thenReturn(List.of());

        GuardianActivationPageResponse page = service.findPage(
                10L, 0, 1, "bruno", "NOT_ACTIVE"
        );

        assertEquals(1, page.content().size());
        assertEquals(2L, page.content().getFirst().guardianId());
        assertEquals(1, page.totalElements());
        assertEquals(1, page.totalPages());
        assertTrue(page.first());
        assertTrue(page.last());
    }

    @Test
    void revokeInvalidatesPendingInvitationsSessionsAndDevices() {
        Guardian guardian = createGuardian(1L);
        GuardianDeviceEnrollment invitation =
                new GuardianDeviceEnrollment();

        GuardianSession session = new GuardianSession();
        session.setExpiresAt(OffsetDateTime.now().plusDays(1));

        GuardianDevice device = new GuardianDevice();
        device.setActive(true);

        List<Long> ids = List.of(1L);
        when(guardianRepository
                .findAllBySchool_IdAndIdInOrderByFullNameAsc(10L, ids))
                .thenReturn(List.of(guardian));
        when(enrollmentRepository
                .findAllByGuardian_IdInAndUsedAtIsNullAndRevokedAtIsNull(ids))
                .thenReturn(List.of(invitation));
        when(guardianSessionRepository
                .findAllByGuardian_IdInAndRevokedAtIsNull(ids))
                .thenReturn(List.of(session));
        when(guardianDeviceRepository
                .findAllByGuardian_IdInAndActiveTrue(ids))
                .thenReturn(List.of(device));

        GuardianAccessRevocationResponse response = service.revoke(
                new RevokeGuardianAccessRequest(10L, ids)
        );

        assertEquals(1, response.invitationsRevoked());
        assertEquals(1, response.sessionsRevoked());
        assertEquals(1, response.devicesDeactivated());
        assertTrue(invitation.getRevokedAt() != null);
        assertTrue(session.getRevokedAt() != null);
        assertFalse(device.isActive());
    }

    @Test
    void revokeRejectsGuardianOutsideRequestedSchoolAtomically() {
        List<Long> ids = List.of(1L, 2L);
        when(guardianRepository
                .findAllBySchool_IdAndIdInOrderByFullNameAsc(10L, ids))
                .thenReturn(List.of(createGuardian(1L)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.revoke(
                        new RevokeGuardianAccessRequest(10L, ids)
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(enrollmentRepository, never())
                .findAllByGuardian_IdInAndUsedAtIsNullAndRevokedAtIsNull(ids);
    }

    private Guardian createGuardian(Long id) {
        School school = new School();
        school.setId(10L);
        school.setName("Escuela");

        Guardian guardian = new Guardian();
        guardian.setId(id);
        guardian.setSchool(school);
        guardian.setFullName("Tutor " + id);
        guardian.setActive(true);
        return guardian;
    }
}
