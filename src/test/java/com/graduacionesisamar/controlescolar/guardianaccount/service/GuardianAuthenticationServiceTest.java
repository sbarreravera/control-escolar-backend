package com.graduacionesisamar.controlescolar.guardianaccount.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardianaccount.dto.GuardianLoginRequest;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardiandevice.service.GuardianDeviceService;
import com.graduacionesisamar.controlescolar.guardiansession.service.GuardianSessionService;
import com.graduacionesisamar.controlescolar.guardiansession.service.IssuedGuardianSession;
import com.graduacionesisamar.controlescolar.school.entity.School;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianAuthenticationServiceTest {

    @Mock
    private GuardianAccountService guardianAccountService;

    @Mock
    private GuardianDeviceService guardianDeviceService;

    @Mock
    private GuardianSessionService guardianSessionService;

    @InjectMocks
    private GuardianAuthenticationService authenticationService;

    @Test
    void loginRecreatesSessionWithoutRequiringNotificationPermission() {
        School school = new School();
        school.setId(10L);
        school.setCode("ESC-TEST-2");
        school.setName("Escuela de Prueba 2");

        Guardian guardian = new Guardian();
        guardian.setId(20L);
        guardian.setSchool(school);
        guardian.setFullName("Tutor de Prueba 4");

        GuardianAccount account = new GuardianAccount();
        account.setGuardian(guardian);
        account.setSchool(school);
        account.setUsername("TUT-020");
        account.setPasswordHash("bcrypt-hash");

        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(90);
        when(guardianAccountService.authenticate(
                "ESC-TEST-2",
                "TUT-020",
                "segura-123"
        )).thenReturn(account);
        when(guardianSessionService.issue(
                guardian,
                null,
                "Teléfono nuevo"
        )).thenReturn(new IssuedGuardianSession("session-token", expiresAt));

        CompletedGuardianLogin result = authenticationService.login(
                new GuardianLoginRequest(
                        "ESC-TEST-2",
                        "TUT-020",
                        "segura-123",
                        null,
                        "Teléfono nuevo"
                )
        );

        assertEquals("session-token", result.sessionToken());
        assertEquals("TUT-020", result.response().username());
        assertEquals("ESC-TEST-2", result.response().schoolCode());
        assertEquals(expiresAt, result.response().sessionExpiresAt());
        assertFalse(result.response().notificationsEnabled());
        assertNull(result.response().device());
        verify(guardianSessionService).issue(
                guardian,
                null,
                "Teléfono nuevo"
        );
        verifyNoInteractions(guardianDeviceService);
    }
}
