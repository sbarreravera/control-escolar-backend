package com.graduacionesisamar.controlescolar.guardianregistration.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianSelfRegistrationRequestValidationTest {

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
    void rejectsBlankEmail() {
        GuardianSelfRegistrationRequest request = requestWithEmail(" ");

        assertThat(validator.validate(request))
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals("email")
                );
    }

    @Test
    void rejectsMalformedEmail() {
        GuardianSelfRegistrationRequest request = requestWithEmail("correo-invalido");

        assertThat(validator.validate(request))
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals("email")
                );
    }

    @Test
    void acceptsValidEmail() {
        GuardianSelfRegistrationRequest request = requestWithEmail(
                "tutor@example.com"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    private GuardianSelfRegistrationRequest requestWithEmail(String email) {
        return new GuardianSelfRegistrationRequest(
                "token",
                null,
                "Tutor Ejemplo",
                "7711234567",
                email,
                "Madre",
                List.of("MAT-001"),
                "password123"
        );
    }
}
