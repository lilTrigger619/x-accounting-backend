package com.unionsg.xaccounting.documenttemplate.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateDesignRequestValidationTest {

    private final Validator validator;

    UpdateDesignRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"#FF0000", "#f00", "#FF0000AA", "rgb(255, 0, 0)", "rgba(255, 0, 0, 0.5)",
            "hsl(0, 100%, 50%)", "red", "gold", "cornflowerblue"})
    void acceptsValidCssColorFormats(String color) {
        UpdateDesignRequest request = new UpdateDesignRequest();
        request.setPrimaryColor(color);

        Set<ConstraintViolation<UpdateDesignRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"red; } </style><script>alert(1)</script>", "javascript:alert(1)",
            "url(javascript:alert(1))", "red\\;background:url(x)"})
    void rejectsUnsafeColorValues(String color) {
        UpdateDesignRequest request = new UpdateDesignRequest();
        request.setPrimaryColor(color);

        Set<ConstraintViolation<UpdateDesignRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void acceptsFontFamilyStack() {
        UpdateDesignRequest request = new UpdateDesignRequest();
        request.setFontFamily("\"Helvetica Neue\", Arial, sans-serif");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsFontFamilyBreakoutAttempt() {
        UpdateDesignRequest request = new UpdateDesignRequest();
        request.setFontFamily("Arial</style><script>alert(1)</script>");

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
