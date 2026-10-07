package com.graduacionesisamar.controlescolar.security.module;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolModuleAccessServiceTest {

    @Mock
    private SchoolAccessService schoolAccessService;

    @InjectMocks
    private SchoolModuleAccessService moduleAccessService;

    @Test
    void operatorCanUseAssignedModule() {
        AppUser user = new AppUser();
        user.setRole(AppUserRole.OPERATOR);
        user.getModulePermissions().add("ACCESS_SCANNER");

        when(schoolAccessService.getCurrentUser())
                .thenReturn(user);

        assertDoesNotThrow(() ->
                moduleAccessService.requireAccess(
                        "ACCESS_SCANNER"
                )
        );
    }

    @Test
    void operatorCannotUseUnassignedModule() {
        AppUser user = new AppUser();
        user.setRole(AppUserRole.OPERATOR);

        when(schoolAccessService.getCurrentUser())
                .thenReturn(user);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> moduleAccessService.requireAccess(
                        "STUDENTS"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );
    }

    @Test
    void administratorAlwaysHasModuleAccess() {
        AppUser user = new AppUser();
        user.setRole(AppUserRole.ADMIN);

        when(schoolAccessService.getCurrentUser())
                .thenReturn(user);

        assertDoesNotThrow(() ->
                moduleAccessService.requireAccess(
                        "FUTURE_MODULE"
                )
        );
    }
}
