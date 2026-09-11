package com.graduacionesisamar.controlescolar.guardianaccount.service;

import com.graduacionesisamar.controlescolar.guardian.entity.Guardian;
import com.graduacionesisamar.controlescolar.guardianaccount.entity.GuardianAccount;
import com.graduacionesisamar.controlescolar.guardianaccount.repository.GuardianAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates guardian usernames and owns password lifecycle rules.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuardianAccountService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final GuardianAccountRepository guardianAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public GuardianAccount ensureAccount(Guardian guardian) {
        return guardianAccountRepository.findByGuardian_Id(guardian.getId())
                .orElseGet(() -> guardianAccountRepository.save(
                        buildAccount(guardian)
                ));
    }

    public List<GuardianAccount> ensureAccounts(List<Guardian> guardians) {
        if (guardians.isEmpty()) {
            return List.of();
        }

        List<Long> guardianIds = guardians.stream()
                .map(Guardian::getId)
                .toList();
        Map<Long, GuardianAccount> byGuardian = guardianAccountRepository
                .findAllByGuardian_IdIn(guardianIds)
                .stream()
                .collect(Collectors.toMap(
                        account -> account.getGuardian().getId(),
                        Function.identity()
                ));

        Long schoolId = guardians.getFirst().getSchool().getId();
        Set<String> usedUsernames = guardianAccountRepository
                .findAllBySchool_Id(schoolId)
                .stream()
                .map(GuardianAccount::getUsername)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<GuardianAccount> missing = guardians.stream()
                .filter(guardian -> !byGuardian.containsKey(guardian.getId()))
                .map(guardian -> buildAccount(guardian, usedUsernames))
                .toList();
        guardianAccountRepository.saveAll(missing).forEach(account ->
                byGuardian.put(account.getGuardian().getId(), account)
        );

        return guardians.stream()
                .map(guardian -> byGuardian.get(guardian.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GuardianAccount findByGuardianId(Long guardianId) {
        return guardianAccountRepository.findByGuardian_Id(guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian account not found"
                ));
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public GuardianAccount authenticate(
            String schoolCode,
            String username,
            String password
    ) {
        GuardianAccount account = guardianAccountRepository
                .findBySchool_CodeIgnoreCaseAndUsernameIgnoreCase(
                        schoolCode.trim(),
                        username.trim()
                )
                .orElseThrow(this::invalidCredentials);

        OffsetDateTime now = OffsetDateTime.now();
        if (account.getLockedUntil() != null
                && account.getLockedUntil().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Guardian account is temporarily locked"
            );
        }

        if (!Boolean.TRUE.equals(account.getActive())
                || !Boolean.TRUE.equals(account.getGuardian().getActive())
                || !Boolean.TRUE.equals(account.getSchool().getActive())
                || !account.isActivated()
                || !passwordEncoder.matches(
                        password,
                        account.getPasswordHash()
                )) {
            registerFailedAttempt(account, now);
            guardianAccountRepository.save(account);
            throw invalidCredentials();
        }

        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        return guardianAccountRepository.save(account);
    }

    public GuardianAccount setPassword(
            GuardianAccount account,
            String password
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setActive(true);
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        account.setPasswordChangedAt(now);
        if (account.getActivatedAt() == null) {
            account.setActivatedAt(now);
        }
        return guardianAccountRepository.save(account);
    }

    private GuardianAccount buildAccount(Guardian guardian) {
        GuardianAccount account = new GuardianAccount();
        account.setGuardian(guardian);
        account.setSchool(guardian.getSchool());
        account.setUsername(resolveAvailableUsername(guardian));
        return account;
    }

    private GuardianAccount buildAccount(
            Guardian guardian,
            Set<String> usedUsernames
    ) {
        String reference = guardian.getExternalReference();
        String candidate = reference == null || reference.isBlank()
                ? "TUTOR-" + guardian.getId()
                : reference.trim().toUpperCase(Locale.ROOT);
        String username = candidate;
        if (!usedUsernames.add(username.toLowerCase(Locale.ROOT))) {
            username = candidate + "-" + guardian.getId();
            usedUsernames.add(username.toLowerCase(Locale.ROOT));
        }

        GuardianAccount account = new GuardianAccount();
        account.setGuardian(guardian);
        account.setSchool(guardian.getSchool());
        account.setUsername(username);
        return account;
    }

    private String resolveAvailableUsername(Guardian guardian) {
        String reference = guardian.getExternalReference();
        String candidate = reference == null || reference.isBlank()
                ? "TUTOR-" + guardian.getId()
                : reference.trim().toUpperCase(Locale.ROOT);

        if (!guardianAccountRepository
                .existsBySchool_IdAndUsernameIgnoreCase(
                        guardian.getSchool().getId(),
                        candidate
                )) {
            return candidate;
        }

        return candidate + "-" + guardian.getId();
    }

    private void registerFailedAttempt(
            GuardianAccount account,
            OffsetDateTime now
    ) {
        int failedAttempts = (account.getFailedAttempts() == null
                ? 0
                : account.getFailedAttempts()) + 1;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            account.setFailedAttempts(0);
            account.setLockedUntil(now.plus(LOCK_DURATION));
            return;
        }
        account.setFailedAttempts(failedAttempts);
        account.setLockedUntil(null);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "School, username or password is incorrect"
        );
    }
}
