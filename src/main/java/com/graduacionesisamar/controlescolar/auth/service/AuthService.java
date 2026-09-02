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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;

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

        School school = appUser.getSchool();

        return new AuthenticatedUserResponse(
                appUser.getId(),
                school == null ? null : school.getId(),
                school == null ? null : school.getName(),
                appUser.getFullName(),
                appUser.getEmail(),
                appUser.getRole()
        );
    }
}