package com.graduacionesisamar.controlescolar.guardianregistration.event;

import java.util.List;

/**
 * Immutable data required to notify a guardian after registration commits.
 */
public record GuardianRegistrationCompletedEvent(
        String recipientEmail,
        String guardianName,
        String guardianReference,
        String username,
        String phone,
        String relationship,
        String schoolName,
        String schoolCode,
        List<String> studentEnrollmentNumbers
) {
}
