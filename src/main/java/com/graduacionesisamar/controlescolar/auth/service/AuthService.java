package com.graduacionesisamar.controlescolar.auth.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.auth.dto.AuthenticatedUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.security.module.SchoolModuleCatalogService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final SchoolModuleCatalogService moduleCatalogService;

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getAuthenticatedUser(
            String email
    ) {
        AppUser appUser = appUserRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        if (!Boolean.TRUE.equals(appUser.getActive())
                || appUser.getArchivedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user is inactive"
            );
        }

        School school = appUser.getSchool();

        List<String> moduleKeys =
                appUser.getRole() == AppUserRole.ADMIN
                        || appUser.getRole() == AppUserRole.SUPER_ADMIN
                        ? moduleCatalogService.findAll()
                                .stream()
                                .map(module -> module.key())
                                .toList()
                        : appUser.getModulePermissions()
                                .stream()
                                .sorted()
                                .toList();

        return new AuthenticatedUserResponse(
                appUser.getId(),
                school == null ? null : school.getId(),
                school == null ? null : school.getName(),
                appUser.getFullName(),
                appUser.getEmail(),
                appUser.getRole(),
                moduleKeys
        );
    }
}