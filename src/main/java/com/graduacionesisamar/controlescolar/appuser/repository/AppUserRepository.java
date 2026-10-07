package com.graduacionesisamar.controlescolar.appuser.repository;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );

    List<AppUser> findAllBySchool_IdOrderByRoleAscFullNameAsc(
            Long schoolId
    );
}