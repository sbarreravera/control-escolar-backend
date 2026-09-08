package com.graduacionesisamar.controlescolar.school.service;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.appuser.repository.AppUserRepository;
import com.graduacionesisamar.controlescolar.school.dto.CreateSchoolRequest;
import com.graduacionesisamar.controlescolar.school.dto.SchoolResponse;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Handles business operations related to schools.
 */
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a school together with its first administrator.
     */
    @Transactional
    public SchoolResponse create(CreateSchoolRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String adminEmail = request.adminEmail().trim().toLowerCase(Locale.ROOT);

        validateCode(code);
        validateAdminEmail(adminEmail);

        School school = new School();
        school.setName(request.name().trim());
        school.setCode(code);

        School savedSchool = schoolRepository.save(school);

        AppUser administrator = new AppUser();
        administrator.setSchool(savedSchool);
        administrator.setFullName(request.adminFullName().trim());
        administrator.setEmail(adminEmail);
        administrator.setPasswordHash(
                passwordEncoder.encode(request.adminPassword())
        );
        administrator.setRole(AppUserRole.ADMIN);
        administrator.setActive(true);

        appUserRepository.save(administrator);

        return toResponse(savedSchool);
    }

    /**
     * Returns all registered schools.
     */
    @Transactional(readOnly = true)
    public List<SchoolResponse> findAll() {
        return schoolRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateCode(String code) {
        if (schoolRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A school with this code already exists"
            );
        }
    }

    private void validateAdminEmail(String email) {
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A user with this email already exists"
            );
        }
    }

    private SchoolResponse toResponse(School school) {
        return new SchoolResponse(
                school.getId(),
                school.getName(),
                school.getCode(),
                school.getActive(),
                school.getCreatedAt(),
                school.getUpdatedAt()
        );
    }
}