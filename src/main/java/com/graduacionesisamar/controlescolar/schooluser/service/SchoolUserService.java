package com.graduacionesisamar.controlescolar.schooluser.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import com.graduacionesisamar.controlescolar.schooluser.dto.CreateSchoolUserRequest;
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
    public List<SchoolUserResponse> findAllBySchool(Long schoolId) {
        schoolAccessService.requireAccessToSchool(schoolId);
        findSchool(schoolId);

        return appUserRepository
                .findAllBySchool_IdOrderByRoleAscFullNameAsc(schoolId)
                .stream()
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
                    "The primary school administrator cannot be edited here"
            );
        }

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

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(
                    passwordEncoder.encode(request.password())
            );
        }

        user.getModulePermissions().clear();
        user.getModulePermissions().addAll(moduleKeys);

        return toResponse(appUserRepository.save(user));
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
                user.getRole() == AppUserRole.OPERATOR,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
