package com.graduacionesisamar.controlescolar.appuser.bootstrap;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.bootstrap-super-admin.enabled",
        havingValue = "true"
)
public class PlatformAdminInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-super-admin.full-name:}")
    private String fullName;

    @Value("${app.bootstrap-super-admin.email:}")
    private String email;

    @Value("${app.bootstrap-super-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateConfiguration();

        String normalizedEmail =
                email.trim().toLowerCase();

        appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresentOrElse(
                        this::validateExistingUser,
                        () -> createSuperAdmin(normalizedEmail)
                );
    }

    private void createSuperAdmin(String normalizedEmail) {
        AppUser superAdmin = new AppUser();
        superAdmin.setSchool(null);
        superAdmin.setFullName(fullName.trim());
        superAdmin.setEmail(normalizedEmail);
        superAdmin.setPasswordHash(
                passwordEncoder.encode(password)
        );
        superAdmin.setRole(AppUserRole.SUPER_ADMIN);
        superAdmin.setActive(true);

        appUserRepository.save(superAdmin);

        log.info(
                "Platform super administrator created: {}",
                normalizedEmail
        );
    }

    private void validateExistingUser(AppUser existingUser) {
        if (existingUser.getRole() != AppUserRole.SUPER_ADMIN) {
            throw new IllegalStateException(
                    "Bootstrap email belongs to a non-platform user"
            );
        }

        log.info(
                "Platform super administrator already exists: {}",
                existingUser.getEmail()
        );
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(fullName)
                || !StringUtils.hasText(email)
                || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "Platform administrator configuration is incomplete"
            );
        }

        if (password.length() < 8) {
            throw new IllegalStateException(
                    "Platform administrator password must contain at least 8 characters"
            );
        }
    }
}