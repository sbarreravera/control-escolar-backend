package com.graduacionesisamar.controlescolar.guardian.service;

import com.graduacionesisamar.controlescolar.guardian.dto.CreateGuardianRequest;
import com.graduacionesisamar.controlescolar.guardian.dto.GuardianResponse;
import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardian.repository.GuardianRepository;
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

    private GuardianService service;
    private School school;

    @BeforeEach
    void setUp() {
        service = new GuardianService(
                guardianRepository,
                schoolRepository,
                schoolAccessService
        );

        school = new School();
        school.setId(10L);
        school.setName("Colegio San Felipe de Jesús");

        when(schoolRepository.findById(10L))
                .thenReturn(Optional.of(school));
    }

    @Test
    void createNormalizesTheOptionalExternalReference() {
        CreateGuardianRequest request = new CreateGuardianRequest(
                10L,
                "  tut-001  ",
                "  María Pérez  ",
                "  773 123 4567  ",
                "  MARIA@EXAMPLE.COM  "
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
        assertEquals("María Pérez", response.fullName());
        assertEquals("773 123 4567", response.phone());
        assertEquals("maria@example.com", response.email());
        verify(schoolAccessService).requireAccessToSchool(10L);
    }

    @Test
    void createRejectsAnExternalReferenceAlreadyUsedInTheSchool() {
        CreateGuardianRequest request = new CreateGuardianRequest(
                10L,
                "TUT-001",
                "María Pérez",
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
}
