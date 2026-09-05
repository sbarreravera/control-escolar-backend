package com.graduacionesisamar.controlescolar.security.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAccessServiceTest {

    private static final String EMAIL = "admin@school.test";

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private SchoolAccessService schoolAccessService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsSchoolUserToAccessAssignedSchool() {
        authenticate();

        when(appUserRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(
                        buildUser(AppUserRole.ADMIN, 1L)
                ));

        assertDoesNotThrow(
                () -> schoolAccessService.requireAccessToSchool(1L)
        );
    }

    @Test
    void rejectsSchoolUserAccessingAnotherSchool() {
        authenticate();

        when(appUserRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(
                        buildUser(AppUserRole.ADMIN, 1L)
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> schoolAccessService.requireAccessToSchool(2L)
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );
    }

    @Test
    void allowsSuperAdminToAccessAnySchool() {
        authenticate();

        when(appUserRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(
                        buildUser(AppUserRole.SUPER_ADMIN, null)
                ));

        assertDoesNotThrow(
                () -> schoolAccessService.requireAccessToSchool(99L)
        );
    }

    @Test
    void rejectsUnauthenticatedRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> schoolAccessService.requireAccessToSchool(1L)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL,
                        "password",
                        List.of()
                )
        );
    }

    private AppUser buildUser(
            AppUserRole role,
            Long schoolId
    ) {
        AppUser user = new AppUser();
        user.setEmail(EMAIL);
        user.setRole(role);
        user.setActive(true);

        if (schoolId != null) {
            School school = new School();
            school.setId(schoolId);
            user.setSchool(school);
        }

        return user;
    }
}