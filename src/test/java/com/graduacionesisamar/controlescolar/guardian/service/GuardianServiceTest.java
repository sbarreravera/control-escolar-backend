package com.graduacionesisamar.controlescolar.guardian.service;

import com.graduacionesisamar.controlescolar.guardian.dto.CreateGuardianRequest;
import com.graduacionesisamar.controlescolar.guardian.dto.GuardianResponse;
import com.graduacionesisamar.controlescolar.guardian.dto.UpdateGuardianRequest;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
import com.graduacionesisamar.controlescolar.guardianaccount.service.GuardianAccountService;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    @Mock
    private GuardianAccountService guardianAccountService;

    private GuardianService service;
    private School school;

    @BeforeEach
    void setUp() {
        service = new GuardianService(
                guardianRepository,
                schoolRepository,
                schoolAccessService,
                guardianAccountService
        );

        school = new School();
        school.setId(10L);
        school.setName("Escuela de Prueba 2");

        lenient().when(schoolRepository.findById(10L))
                .thenReturn(Optional.of(school));
    }

    @Test
    void createNormalizesTheOptionalExternalReference() {
        CreateGuardianRequest request = new CreateGuardianRequest(
                10L,
                "  tut-001  ",
                "  Tutor de Prueba 4  ",
                "  5550000401  ",
                "  guardian.1@example.test  "
        );

        when(guardianRepository
                .existsBySchool_IdAndExternalReferenceIgnoreCase(
                        10L,
                        "TUT-001"
                ))
                .thenReturn(false);
        when(guardianRepository.save(any(Guardian.class)))
                .thenAnswer(invocation -> {
                    Guardian guardian = invocation.getArgument(0);
                    guardian.setId(20L);
                    return guardian;
                });

        GuardianResponse response = service.create(request);

        assertEquals("TUT-001", response.externalReference());
        assertEquals("Tutor de Prueba 4", response.fullName());
        assertEquals("5550000401", response.phone());
        assertEquals("guardian.1@example.test", response.email());
        verify(schoolAccessService).requireAccessToSchool(10L);
        verify(guardianAccountService).ensureAccount(any(Guardian.class));
    }

    @Test
    void createRejectsAnExternalReferenceAlreadyUsedInTheSchool() {
        CreateGuardianRequest request = new CreateGuardianRequest(
                10L,
                "TUT-001",
                "Tutor de Prueba 4",
                null,
                null
        );

        when(guardianRepository
                .existsBySchool_IdAndExternalReferenceIgnoreCase(
                        10L,
                        "TUT-001"
                ))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(request)
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(guardianRepository, never()).save(any(Guardian.class));
    }

    @Test
    void updateChangesContactDataAndPreservesStableIdentity() {
        Guardian guardian = new Guardian();
        guardian.setId(20L);
        guardian.setSchool(school);
        guardian.setExternalReference("TUT-PRUEBA-001");
        guardian.setFullName("Tutor de Prueba 2");

        UpdateGuardianRequest request = new UpdateGuardianRequest(
                "  Tutor de Prueba 2  ",
                "  5550000402  ",
                "  guardian.3@example.test  "
        );

        when(guardianRepository.findById(20L))
                .thenReturn(Optional.of(guardian));
        when(guardianRepository.save(guardian))
                .thenReturn(guardian);

        GuardianResponse response = service.update(20L, request);

        assertEquals("TUT-PRUEBA-001", response.externalReference());
        assertEquals("Tutor de Prueba 2", response.fullName());
        assertEquals("5550000402", response.phone());
        assertEquals("guardian.3@example.test", response.email());
        verify(schoolAccessService).requireAccessToSchool(10L);
        verify(guardianRepository).save(guardian);
    }

    @Test
    void updateRejectsAnUnknownGuardian() {
        UpdateGuardianRequest request = new UpdateGuardianRequest(
                "Tutor de Prueba 2",
                null,
                null
        );

        when(guardianRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.update(99L, request)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(guardianRepository, never()).save(any(Guardian.class));
    }
}
