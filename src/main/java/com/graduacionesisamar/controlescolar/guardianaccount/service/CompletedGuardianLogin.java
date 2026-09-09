package com.graduacionesisamar.controlescolar.guardianaccount.service;

import com.graduacionesisamar.controlescolar.guardianaccount.dto.GuardianLoginResponse;

/**
 * Public response plus the raw opaque token written only to HttpOnly cookie.
 */
public record CompletedGuardianLogin(
        GuardianLoginResponse response,
        String sessionToken
) {
}
