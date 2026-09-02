package com.graduacionesisamar.controlescolar.school.repository;

import com.graduacionesisamar.controlescolar.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<School> findByCodeIgnoreCase(String code);
}