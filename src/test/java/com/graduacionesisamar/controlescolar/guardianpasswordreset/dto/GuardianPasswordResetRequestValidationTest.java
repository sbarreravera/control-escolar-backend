package com.graduacionesisamar.controlescolar.guardianpasswordreset.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianPasswordResetRequestValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void rejectsBlankSchoolCode() {
        assertThat(validator.validate(new GuardianPasswordResetRequest(
                " ",
                "tutor@example.com"
        ))).isNotEmpty();
    }

    @Test
    void rejectsMalformedEmail() {
        assertThat(validator.validate(new GuardianPasswordResetRequest(
                "ESC-001",
                "correo-invalido"
        ))).isNotEmpty();
    }

    @Test
    void acceptsValidRequest() {
        assertThat(validator.validate(new GuardianPasswordResetRequest(
                "ESC-001",
                "tutor@example.com"
        ))).isEmpty();
    }
}
