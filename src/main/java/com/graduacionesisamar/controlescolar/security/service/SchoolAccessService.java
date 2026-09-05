package com.graduacionesisamar.controlescolar.security.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Verifies that the authenticated user can access a school's data.
 */
@Service
@RequiredArgsConstructor
public class SchoolAccessService {

    private final AppUserRepository appUserRepository;

    /**
     * Allows platform administrators to access any school and restricts
     * school users to the school assigned to their account.
     */
    @Transactional(readOnly = true)
    public void requireAccessToSchool(Long schoolId) {
        AppUser currentUser = findCurrentUser();

        if (currentUser.getRole() == AppUserRole.SUPER_ADMIN) {
            return;
        }

        Long assignedSchoolId = currentUser.getSchool() == null
                ? null
                : currentUser.getSchool().getId();

        if (!Objects.equals(assignedSchoolId, schoolId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "School access denied"
            );
        }
    }

    private AppUser findCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw unauthorized();
        }

        AppUser currentUser = appUserRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(this::unauthorized);

        if (!Boolean.TRUE.equals(currentUser.getActive())) {
            throw unauthorized();
        }

        return currentUser;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated user not found"
        );
    }
}