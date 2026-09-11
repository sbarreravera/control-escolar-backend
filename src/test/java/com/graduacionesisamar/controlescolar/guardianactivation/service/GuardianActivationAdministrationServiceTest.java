package com.graduacionesisamar.controlescolar.guardianactivation.service;

import com.graduacionesisamar.controlescolar.academiccycle.entity.AcademicCycle;
import com.graduacionesisamar.controlescolar.academiccycle.repository.AcademicCycleRepository;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardianaccount.repository.GuardianAccountRepository;
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
import com.graduacionesisamar.controlescolar.studentguardian.repository.StudentGuardianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
    private AcademicCycleRepository academicCycleRepository;
    @Mock
    private StudentGuardianRepository studentGuardianRepository;
    @Mock
    private SchoolAccessService schoolAccessService;
    @Mock
    private GuardianAccountRepository guardianAccountRepository;

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

        stubAcademicScope();
        when(guardianRepository.findForActivation(
                10L, 30L, null, "", "ALL", ""
        ))
                .thenReturn(List.of(guardian));
        when(enrollmentRepository
                .findAllByGuardian_IdInOrderByCreatedAtDesc(List.of(1L)))
                .thenReturn(List.of(enrollment));
        when(guardianSessionRepository
                .findAllByGuardian_IdIn(List.of(1L)))
                .thenReturn(List.of(session));
        when(guardianDeviceRepository
                .findAllByGuardian_IdIn(List.of(1L)))
                .thenReturn(List.of(device));
        when(guardianAccountRepository.findAllByGuardian_IdIn(List.of(1L)))
                .thenReturn(List.of(activatedAccount(guardian)));
        when(studentGuardianRepository.findForGuardianSummaries(
                List.of(1L), 30L
        )).thenReturn(List.of());

        GuardianActivationPageResponse page = service.findPage(
                10L, 0, 25, "", "ALL", 30L, null, "", "ALL"
        );
        List<GuardianActivationStatusResponse> statuses = page.content();

        assertEquals(1, statuses.size());
        assertEquals(1, page.totalElements());
        assertEquals("ACTIVE", statuses.getFirst().activationState());
        assertEquals(1, statuses.getFirst().activeSessions());
        assertEquals(1, statuses.getFirst().activeDevices());
        assertEquals(1, page.summary().active());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void findPageFiltersBeforeApplyingPagination() {
        Guardian second = createGuardian(2L);
        second.setFullName("Tutor de Prueba 5");

        stubAcademicScope();
        when(guardianRepository.findForActivation(
                10L, 30L, null, "", "ALL", "bruno"
        )).thenReturn(List.of(second));
        when(enrollmentRepository
                .findAllByGuardian_IdInOrderByCreatedAtDesc(List.of(2L)))
                .thenReturn(List.of());
        when(guardianSessionRepository
                .findAllByGuardian_IdIn(List.of(2L)))
                .thenReturn(List.of());
        when(guardianDeviceRepository
                .findAllByGuardian_IdIn(List.of(2L)))
                .thenReturn(List.of());
        when(studentGuardianRepository.findForGuardianSummaries(
                List.of(2L), 30L
        )).thenReturn(List.of());

        GuardianActivationPageResponse page = service.findPage(
                10L,
                0,
                1,
                "bruno",
                "NOT_ACTIVE",
                30L,
                null,
                "",
                "ALL"
        );

        assertEquals(1, page.content().size());
        assertEquals(2L, page.content().getFirst().guardianId());
        assertEquals(1, page.totalElements());
        assertEquals(1, page.totalPages());
        assertTrue(page.first());
        assertTrue(page.last());
        assertEquals(1, page.summary().requiresActivation());
    }

    @Test
    void findSelectionReturnsAllActiveGuardiansMatchingTheScope() {
        Guardian active = createGuardian(1L);
        Guardian inactive = createGuardian(2L);
        inactive.setActive(false);
        List<Long> ids = List.of(1L, 2L);

        stubAcademicScope();
        when(guardianRepository.findForActivation(
                10L, 30L, null, "", "ALL", ""
        )).thenReturn(List.of(active, inactive));
        when(enrollmentRepository
                .findAllByGuardian_IdInOrderByCreatedAtDesc(ids))
                .thenReturn(List.of());
        when(guardianSessionRepository.findAllByGuardian_IdIn(ids))
                .thenReturn(List.of());
        when(guardianDeviceRepository.findAllByGuardian_IdIn(ids))
                .thenReturn(List.of());
        when(studentGuardianRepository.findForGuardianSummaries(ids, 30L))
                .thenReturn(List.of());

        var selection = service.findSelection(
                10L, "", "ALL", 30L, null, "", "ALL"
        );

        assertEquals(List.of(1L), selection.guardianIds());
        assertEquals(1, selection.totalSelected());
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

    @Test
    void revokeSessionClosesEverySessionAttachedToTheSelectedDevice() {
        Guardian guardian = createGuardian(1L);
        GuardianSession selected = new GuardianSession();
        selected.setId(100L);
        selected.setGuardian(guardian);
        selected.setGuardianDeviceId(50L);
        selected.setExpiresAt(OffsetDateTime.now().plusDays(10));

        GuardianSession sibling = new GuardianSession();
        sibling.setId(101L);
        sibling.setGuardian(guardian);
        sibling.setGuardianDeviceId(50L);
        sibling.setExpiresAt(OffsetDateTime.now().plusDays(10));

        GuardianDevice device = new GuardianDevice();
        device.setId(50L);
        device.setGuardian(guardian);
        device.setActive(true);

        when(guardianRepository.findById(1L))
                .thenReturn(Optional.of(guardian));
        when(guardianSessionRepository.findByIdAndGuardian_Id(100L, 1L))
                .thenReturn(Optional.of(selected));
        when(guardianSessionRepository
                .findAllByGuardianDeviceIdAndRevokedAtIsNull(50L))
                .thenReturn(List.of(selected, sibling));
        when(guardianDeviceRepository.findByIdAndGuardian_Id(50L, 1L))
                .thenReturn(Optional.of(device));

        GuardianAccessRevocationResponse response = service.revokeSession(
                1L,
                100L
        );

        assertEquals(2, response.sessionsRevoked());
        assertEquals(1, response.devicesDeactivated());
        assertTrue(selected.getRevokedAt() != null);
        assertTrue(sibling.getRevokedAt() != null);
        assertFalse(device.isActive());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    private Guardian createGuardian(Long id) {
        School school = new School();
        school.setId(10L);
        school.setName("Escuela de Prueba 3");

        Guardian guardian = new Guardian();
        guardian.setId(id);
        guardian.setSchool(school);
        guardian.setFullName("Tutor de Prueba 6" + id);
        guardian.setActive(true);
        return guardian;
    }

    private GuardianAccount activatedAccount(Guardian guardian) {
        GuardianAccount account = new GuardianAccount();
        account.setGuardian(guardian);
        account.setSchool(guardian.getSchool());
        account.setUsername("TUT-" + guardian.getId());
        account.setPasswordHash("hash");
        return account;
    }

    private void stubAcademicScope() {
        School school = new School();
        school.setId(10L);

        AcademicCycle cycle = new AcademicCycle();
        cycle.setId(30L);
        cycle.setSchool(school);

        when(schoolRepository.existsById(10L)).thenReturn(true);
        when(academicCycleRepository.findById(30L))
                .thenReturn(Optional.of(cycle));
    }
}
