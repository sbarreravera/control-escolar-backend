package com.graduacionesisamar.controlescolar.schooluser.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.schooluser.dto.CreateSchoolUserRequest;
import com.graduacionesisamar.controlescolar.schooluser.dto.SchoolUserResponse;
import com.graduacionesisamar.controlescolar.security.module.SchoolModuleCatalogService;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private SchoolAccessService schoolAccessService;

    @Mock
    private SchoolModuleCatalogService moduleCatalogService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SchoolUserService service;
    private School school;

    @BeforeEach
    void setUp() {
        service = new SchoolUserService(
                appUserRepository,
                schoolRepository,
                schoolAccessService,
                moduleCatalogService,
                passwordEncoder
        );

        school = new School();
        school.setId(1L);
        school.setName("Escuela de prueba");
        school.setActive(true);
    }

    @Test
    void createBuildsOperatorWithSelectedModules() {
        when(schoolRepository.findById(1L))
                .thenReturn(Optional.of(school));
        when(appUserRepository.existsByEmailIgnoreCase(
                "prefecto@escuela.mx"
        )).thenReturn(false);
        when(moduleCatalogService.findAllKeys())
                .thenReturn(Set.of(
                        "ACCESS_SCANNER",
                        "STUDENTS"
                ));
        when(passwordEncoder.encode("Temporal#123"))
                .thenReturn("encoded");
        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> {
                    AppUser user = invocation.getArgument(0);
                    user.setId(20L);
                    return user;
                });

        SchoolUserResponse response = service.create(
                new CreateSchoolUserRequest(
                        1L,
                        "Prefecto Uno",
                        "PREFECTO@ESCUELA.MX",
                        "Temporal#123",
                        Set.of("ACCESS_SCANNER")
                )
        );

        assertEquals(20L, response.id());
        assertEquals(AppUserRole.OPERATOR, response.role());
        assertEquals(
                Set.of("ACCESS_SCANNER"),
                Set.copyOf(response.moduleKeys())
        );
        assertTrue(response.active());
        assertTrue(response.editable());

        verify(schoolAccessService).requireAccessToSchool(1L);
    }

    @Test
    void createRejectsUnknownModule() {
        when(schoolRepository.findById(1L))
                .thenReturn(Optional.of(school));
        when(appUserRepository.existsByEmailIgnoreCase(
                "prefecto@escuela.mx"
        )).thenReturn(false);
        when(moduleCatalogService.findAllKeys())
                .thenReturn(Set.of("ACCESS_SCANNER"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        new CreateSchoolUserRequest(
                                1L,
                                "Prefecto Uno",
                                "prefecto@escuela.mx",
                                "Temporal#123",
                                Set.of("UNKNOWN_MODULE")
                        )
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }
}
