package com.graduacionesisamar.controlescolar.guardianaccount.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardianaccount.repository.GuardianAccountRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianAccountServiceTest {

    @Mock
    private GuardianAccountRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void ensureAccountUsesTheStableGuardianReferenceAsUsername() {
        Guardian guardian = guardian();
        when(repository.findByGuardian_Id(20L)).thenReturn(Optional.empty());
        when(repository.existsBySchool_IdAndUsernameIgnoreCase(
                10L,
                "TUT-001"
        )).thenReturn(false);
        when(repository.save(any(GuardianAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GuardianAccountService service = service();
        GuardianAccount result = service.ensureAccount(guardian);

        assertEquals("TUT-001", result.getUsername());
        assertEquals(guardian, result.getGuardian());
        assertEquals(guardian.getSchool(), result.getSchool());
    }

    @Test
    void setPasswordStoresOnlyTheEncodedValueAndActivatesAccount() {
        GuardianAccount account = account(true);
        when(passwordEncoder.encode("secret-123"))
                .thenReturn("bcrypt-hash");
        when(repository.save(account)).thenReturn(account);

        GuardianAccount result = service().setPassword(
                account,
                "secret-123"
        );

        assertEquals("bcrypt-hash", result.getPasswordHash());
        assertNotNull(result.getActivatedAt());
        assertNotNull(result.getPasswordChangedAt());
        assertEquals(0, result.getFailedAttempts());
    }

    @Test
    void authenticateLocksTheAccountAfterFiveFailures() {
        GuardianAccount account = account(true);
        account.setPasswordHash("bcrypt-hash");
        account.setFailedAttempts(4);
        when(repository.findBySchool_CodeIgnoreCaseAndUsernameIgnoreCase(
                "ESC-TEST-2",
                "TUT-001"
        )).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("incorrecta", "bcrypt-hash"))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service().authenticate(
                        "ESC-TEST-2",
                        "TUT-001",
                        "incorrecta"
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals(0, account.getFailedAttempts());
        assertNotNull(account.getLockedUntil());
        verify(repository).save(account);
    }

    @Test
    void authenticateResetsPreviousFailuresAfterSuccess() {
        GuardianAccount account = account(true);
        account.setPasswordHash("bcrypt-hash");
        account.setFailedAttempts(2);
        when(repository.findBySchool_CodeIgnoreCaseAndUsernameIgnoreCase(
                "ESC-TEST-2",
                "TUT-001"
        )).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret-123", "bcrypt-hash"))
                .thenReturn(true);
        when(repository.save(account)).thenReturn(account);

        GuardianAccount result = service().authenticate(
                "ESC-TEST-2",
                "TUT-001",
                "secret-123"
        );

        assertEquals(account, result);
        assertEquals(0, result.getFailedAttempts());
    }

    private GuardianAccountService service() {
        return new GuardianAccountService(repository, passwordEncoder);
    }

    private Guardian guardian() {
        School school = new School();
        school.setId(10L);
        school.setCode("ESC-TEST-2");
        school.setName("Escuela de Prueba 2");
        school.setActive(true);

        Guardian guardian = new Guardian();
        guardian.setId(20L);
        guardian.setSchool(school);
        guardian.setExternalReference("TUT-001");
        guardian.setFullName("Tutor de Prueba 3");
        guardian.setActive(true);
        return guardian;
    }

    private GuardianAccount account(boolean active) {
        Guardian guardian = guardian();
        GuardianAccount account = new GuardianAccount();
        account.setId(30L);
        account.setGuardian(guardian);
        account.setSchool(guardian.getSchool());
        account.setUsername("TUT-001");
        account.setActive(active);
        account.setFailedAttempts(0);
        return account;
    }
}
