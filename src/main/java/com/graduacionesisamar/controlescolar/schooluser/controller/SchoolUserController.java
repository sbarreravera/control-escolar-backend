package com.graduacionesisamar.controlescolar.schooluser.controller;

import com.graduacionesisamar.controlescolar.schooluser.dto.CreateSchoolUserRequest;
import com.graduacionesisamar.controlescolar.schooluser.dto.SchoolModuleResponse;
import com.graduacionesisamar.controlescolar.schooluser.dto.SchoolUserResponse;
import com.graduacionesisamar.controlescolar.schooluser.dto.UpdateSchoolUserRequest;
import com.graduacionesisamar.controlescolar.schooluser.service.SchoolUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/school-users")
@RequiredArgsConstructor
@Validated
public class SchoolUserController {

    private final SchoolUserService schoolUserService;

    @GetMapping("/modules")
    public List<SchoolModuleResponse> findModules() {
        return schoolUserService.findAvailableModules();
    }

    @GetMapping
    public List<SchoolUserResponse> findAll(
            @RequestParam @Positive Long schoolId
    ) {
        return schoolUserService.findAllBySchool(schoolId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolUserResponse create(
            @Valid @RequestBody CreateSchoolUserRequest request
    ) {
        return schoolUserService.create(request);
    }

    @PutMapping("/{userId}")
    public SchoolUserResponse update(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateSchoolUserRequest request
    ) {
        return schoolUserService.update(userId, request);
    }
}
