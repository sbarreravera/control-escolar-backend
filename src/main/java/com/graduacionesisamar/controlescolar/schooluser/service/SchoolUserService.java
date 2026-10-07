package com.graduacionesisamar.controlescolar.schooluser.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.appuser.service.AppUserSessionService;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.schooluser.dto.CreateSchoolUserRequest;
import com.graduacionesisamar.controlescolar.schooluser.dto.RestoreSchoolUserRequest;
import com.graduacionesisamar.controlescolar.schooluser.dto.SchoolModuleResponse;
import com.graduacionesisamar.controlescolar.schooluser.dto.SchoolUserResponse;
import com.graduacionesisamar.controlescolar.schooluser.dto.UpdateSchoolUserRequest;
import com.graduacionesisamar.controlescolar.security.module.SchoolModuleCatalogService;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SchoolUserService {

    private final AppUserRepository appUserRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolAccessService schoolAccessService;
    private final SchoolModuleCatalogService moduleCatalogService;
    private final PasswordEncoder passwordEncoder;
    private final AppUserSessionService appUserSessionService;

    @Transactional(readOnly = true)
    public List<SchoolModuleResponse> findAvailableModules() {
        return moduleCatalogService.findAll()
                .stream()
                .map(module -> new SchoolModuleResponse(
                        module.key(),
                        module.name(),
                        module.description(),
                        module.defaultGranted(),
                        module.order()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SchoolUserResponse> findAllBySchool(
            Long schoolId,
            boolean archived,
            String search
    ) {
        schoolAccessService.requireAccessToSchool(schoolId);
        findSchool(schoolId);

        List<AppUser> users = archived
                ? appUserRepository
                .findAllBySchool_IdAndArchivedAtIsNotNullOrderByArchivedAtDescFullNameAsc(
                        schoolId
                )
                : appUserRepository
                .findAllBySchool_IdAndArchivedAtIsNullOrderByRoleAscFullNameAsc(
                        schoolId
                );

        String normalizedSearch = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);

        return users.stream()
                .filter(user -> matchesSearch(
                        user,
                        normalizedSearch
                ))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SchoolUserResponse create(CreateSchoolUserRequest request) {
        schoolAccessService.requireAccessToSchool(request.schoolId());

        School school = findSchool(request.schoolId());

        if (!Boolean.TRUE.equals(school.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "School is inactive"
            );
        }

        String normalizedEmail = normalizeEmail(request.email());

        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A user with this email already exists"
            );
        }

        Set<String> moduleKeys = validateModuleKeys(request.moduleKeys());

        AppUser user = new AppUser();
        user.setSchool(school);
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(AppUserRole.OPERATOR);
        user.setActive(true);
        user.getModulePermissions().addAll(moduleKeys);

        return toResponse(appUserRepository.save(user));
    }

    @Transactional
    public SchoolUserResponse update(
            Long userId,
            UpdateSchoolUserRequest request
    ) {
        AppUser user = findUser(userId);

        requireEditableUser(user);

        if (user.getArchivedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Archived users must be restored before editing"
            );
        }

        String previousEmail = user.getEmail();
        boolean passwordChanged =
                request.password() != null
                        && !request.password().isBlank();

        String normalizedEmail = normalizeEmail(request.email());

        if (appUserRepository.existsByEmailIgnoreCaseAndIdNot(
                normalizedEmail,
                user.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A user with this email already exists"
            );
        }

        Set<String> moduleKeys = validateModuleKeys(request.moduleKeys());

        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setActive(request.active());

        if (passwordChanged) {
            user.setPasswordHash(
                    passwordEncoder.encode(request.password())
            );
        }

        user.getModulePermissions().clear();
        user.getModulePermissions().addAll(moduleKeys);

        AppUser savedUser = appUserRepository.save(user);

        boolean emailChanged =
                !previousEmail.equalsIgnoreCase(normalizedEmail);

        if (!request.active() || passwordChanged || emailChanged) {
            appUserSessionService.invalidateAllForPrincipal(
                    previousEmail
            );

            if (emailChanged) {
                appUserSessionService.invalidateAllForPrincipal(
                        normalizedEmail
                );
            }
        }

        return toResponse(savedUser);
    }

    @Transactional
    public SchoolUserResponse archive(Long userId) {
        AppUser user = findUser(userId);

        requireEditableUser(user);

        if (user.getArchivedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "School user is already archived"
            );
        }

        AppUser archivedBy = schoolAccessService.getCurrentUser();

        user.setActive(false);
        user.setArchivedAt(OffsetDateTime.now());
        user.setArchivedBy(archivedBy);

        AppUser savedUser = appUserRepository.save(user);

        appUserSessionService.invalidateAllForPrincipal(
                user.getEmail()
        );

        return toResponse(savedUser);
    }

    @Transactional
    public SchoolUserResponse restore(
            Long userId,
            RestoreSchoolUserRequest request
    ) {
        AppUser user = findUser(userId);

        requireEditableUser(user);

        if (user.getArchivedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "School user is not archived"
            );
        }

        School school = user.getSchool();

        if (!Boolean.TRUE.equals(school.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "School is inactive"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setActive(true);
        user.setArchivedAt(null);
        user.setArchivedBy(null);

        return toResponse(appUserRepository.save(user));
    }

    private void requireEditableUser(AppUser user) {
        if (user.getSchool() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "School user not found"
            );
        }

        schoolAccessService.requireAccessToSchool(
                user.getSchool().getId()
        );

        if (user.getRole() != AppUserRole.OPERATOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The primary school administrator cannot be archived or edited here"
            );
        }
    }

    private boolean matchesSearch(
            AppUser user,
            String normalizedSearch
    ) {
        if (normalizedSearch.isBlank()) {
            return true;
        }

        return user.getFullName()
                .toLowerCase(Locale.ROOT)
                .contains(normalizedSearch)
                || user.getEmail()
                .toLowerCase(Locale.ROOT)
                .contains(normalizedSearch);
    }

    private Set<String> validateModuleKeys(Collection<String> values) {
        Set<String> availableKeys = moduleCatalogService.findAllKeys();
        Set<String> normalizedKeys = new LinkedHashSet<>();

        for (String value : values) {
            String normalized = value
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!availableKeys.contains(normalized)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown school module: " + normalized
                );
            }

            normalizedKeys.add(normalized);
        }

        return normalizedKeys;
    }

    private School findSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School not found"
                ));
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "School user not found"
                ));
    }

    private SchoolUserResponse toResponse(AppUser user) {
        List<String> moduleKeys =
                user.getRole() == AppUserRole.ADMIN
                        ? moduleCatalogService.findAll()
                        .stream()
                        .map(module -> module.key())
                        .toList()
                        : user.getModulePermissions()
                        .stream()
                        .sorted()
                        .toList();

        AppUser archivedBy = user.getArchivedBy();
        boolean archived = user.getArchivedAt() != null;
        boolean configurable = user.getRole() == AppUserRole.OPERATOR;

        return new SchoolUserResponse(
                user.getId(),
                user.getSchool() == null
                        ? null
                        : user.getSchool().getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                Boolean.TRUE.equals(user.getActive()),
                moduleKeys,
                configurable && !archived,
                configurable && !archived,
                configurable && archived,
                user.getArchivedAt(),
                archivedBy == null ? null : archivedBy.getId(),
                archivedBy == null ? null : archivedBy.getFullName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
