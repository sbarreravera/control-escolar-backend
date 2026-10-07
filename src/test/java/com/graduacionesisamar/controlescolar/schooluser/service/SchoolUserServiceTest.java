package com.graduacionesisamar.controlescolar.schooluser.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.appuser.service.AppUserSessionService;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.schooluser.dto.CreateSchoolUserRequest;
import com.graduacionesisamar.controlescolar.schooluser.dto.RestoreSchoolUserRequest;
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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Mock
    private AppUserSessionService appUserSessionService;

    private SchoolUserService service;
    private School school;

    @BeforeEach
    void setUp() {
        service = new SchoolUserService(
                appUserRepository,
                schoolRepository,
                schoolAccessService,
                moduleCatalogService,
                passwordEncoder,
                appUserSessionService
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
        assertTrue(response.archivable());
        assertFalse(response.restorable());
        assertNull(response.archivedAt());

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

    @Test
    void archiveDisablesOperatorAndKeepsAuditActor() {
        AppUser user = buildOperator(20L);
        AppUser admin = new AppUser();
        admin.setId(10L);
        admin.setFullName("Directora Principal");
        admin.setRole(AppUserRole.ADMIN);

        when(appUserRepository.findById(20L))
                .thenReturn(Optional.of(user));
        when(schoolAccessService.getCurrentUser())
                .thenReturn(admin);
        when(appUserRepository.save(user))
                .thenReturn(user);

        SchoolUserResponse response = service.archive(20L);

        assertFalse(response.active());
        assertFalse(response.editable());
        assertFalse(response.archivable());
        assertTrue(response.restorable());
        assertNotNull(response.archivedAt());
        assertEquals(10L, response.archivedByUserId());
        assertEquals(
                "Directora Principal",
                response.archivedByUserName()
        );
        assertEquals(admin, user.getArchivedBy());

        verify(schoolAccessService).requireAccessToSchool(1L);
        verify(appUserSessionService)
                .invalidateAllForPrincipal(
                        "prefecto@escuela.mx"
                );
    }

    @Test
    void restoreRequiresNewPasswordAndPreservesPermissions() {
        AppUser user = buildOperator(20L);
        user.setActive(false);
        user.setArchivedAt(OffsetDateTime.now().minusDays(1));
        user.setArchivedBy(new AppUser());

        when(appUserRepository.findById(20L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NuevaClave#2026"))
                .thenReturn("new-hash");
        when(appUserRepository.save(user))
                .thenReturn(user);

        SchoolUserResponse response = service.restore(
                20L,
                new RestoreSchoolUserRequest("NuevaClave#2026")
        );

        assertTrue(response.active());
        assertTrue(response.editable());
        assertTrue(response.archivable());
        assertFalse(response.restorable());
        assertNull(response.archivedAt());
        assertNull(user.getArchivedBy());
        assertEquals("new-hash", user.getPasswordHash());
        assertEquals(
                Set.of("ACCESS_SCANNER"),
                Set.copyOf(response.moduleKeys())
        );

        verify(schoolAccessService).requireAccessToSchool(1L);
    }

    @Test
    void primaryAdministratorCannotBeArchived() {
        AppUser user = new AppUser();
        user.setId(5L);
        user.setSchool(school);
        user.setRole(AppUserRole.ADMIN);

        when(appUserRepository.findById(5L))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.archive(5L)
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );
    }

    private AppUser buildOperator(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setSchool(school);
        user.setFullName("Prefecto Uno");
        user.setEmail("prefecto@escuela.mx");
        user.setPasswordHash("old-hash");
        user.setRole(AppUserRole.OPERATOR);
        user.setActive(true);
        user.getModulePermissions().add("ACCESS_SCANNER");
        return user;
    }
}
