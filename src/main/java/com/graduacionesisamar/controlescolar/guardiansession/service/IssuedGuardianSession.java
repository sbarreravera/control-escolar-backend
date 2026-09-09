package com.graduacionesisamar.controlescolar.guardiansession.service;

import java.time.OffsetDateTime;

/**
 * Raw session token returned only at issuance so it can be placed
 * in an HttpOnly cookie.
 */
public record IssuedGuardianSession(
        String token,
        OffsetDateTime expiresAt
) {
}
