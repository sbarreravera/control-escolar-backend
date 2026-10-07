package com.graduacionesisamar.controlescolar.security.module;

import com.graduacionesisamar.controlescolar.appuser.entity.AppUser;
import com.graduacionesisamar.controlescolar.appuser.entity.AppUserRole;
import com.graduacionesisamar.controlescolar.security.service.SchoolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SchoolModuleAccessService {

    private final SchoolAccessService schoolAccessService;

    @Transactional(readOnly = true)
    public void requireAccess(String moduleKey) {
        AppUser currentUser = schoolAccessService.getCurrentUser();

        if (currentUser.getRole() == AppUserRole.SUPER_ADMIN
                || currentUser.getRole() == AppUserRole.ADMIN) {
            return;
        }

        String normalizedKey = moduleKey.trim().toUpperCase();

        if (currentUser.getRole() == AppUserRole.OPERATOR
                && currentUser.getModulePermissions().contains(normalizedKey)) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "School module access denied"
        );
    }
}
