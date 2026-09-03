package com.graduacionesisamar.controlescolar.school.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.school.dto.CreateSchoolRequest;
import com.graduacionesisamar.controlescolar.school.dto.SchoolResponse;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SchoolService schoolService;
    private CreateSchoolRequest request;

    @BeforeEach
    void setUp() {
        schoolService = new SchoolService(
                schoolRepository,
                appUserRepository,
                passwordEncoder
        );

        request = new CreateSchoolRequest(
                "  Colegio Ejemplo  ",
                " col-001 ",
                "  Administrador Escolar  ",
                "ADMIN@EJEMPLO.COM",
                "password123"
        );
    }

    @Test
    void createRegistersSchoolAndItsFirstAdministrator() {
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(schoolRepository.save(any(School.class)))
                .thenAnswer(invocation -> {
                    School school = invocation.getArgument(0);
                    school.setId(10L);
                    school.beforeInsert();
                    return school;
                });

        SchoolResponse response = schoolService.create(request);

        assertEquals(10L, response.id());
        assertEquals("Colegio Ejemplo", response.name());
        assertEquals("COL-001", response.code());
        assertTrue(response.active());

        ArgumentCaptor<AppUser> administratorCaptor =
                ArgumentCaptor.forClass(AppUser.class);

        verify(appUserRepository).save(administratorCaptor.capture());

        AppUser administrator = administratorCaptor.getValue();

        assertEquals(10L, administrator.getSchool().getId());
        assertEquals(
                "Administrador Escolar",
                administrator.getFullName()
        );
        assertEquals(
                "admin@ejemplo.com",
                administrator.getEmail()
        );
        assertEquals(
                "encoded-password",
                administrator.getPasswordHash()
        );
        assertEquals(AppUserRole.ADMIN, administrator.getRole());
        assertTrue(administrator.getActive());
    }

    @Test
    void createRejectsDuplicatedSchoolCode() {
        when(schoolRepository.existsByCodeIgnoreCase("COL-001"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> schoolService.create(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(schoolRepository, never()).save(any());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicatedAdministratorEmail() {
        when(appUserRepository.existsByEmailIgnoreCase(
                "admin@ejemplo.com"
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> schoolService.create(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(schoolRepository, never()).save(any());
        verify(appUserRepository, never()).save(any());
    }
}