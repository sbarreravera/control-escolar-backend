package com.graduacionesisamar.controlescolar.school.service;

import com.graduacionesisamar.controlescolar.school.dto.CreateSchoolRequest;
import com.graduacionesisamar.controlescolar.school.dto.SchoolResponse;
import com.graduacionesisamar.controlescolar.school.entity.School;
import com.graduacionesisamar.controlescolar.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Handles business operations related to schools.
 */
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    /**
     * Creates a new school.
     */
    public SchoolResponse create(CreateSchoolRequest request) {
        String code = request.code().trim().toUpperCase();

        validateCode(code);

        School school = new School();
        school.setName(request.name().trim());
        school.setCode(code);

        return toResponse(schoolRepository.save(school));
    }

    /**
     * Returns all registered schools.
     */
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