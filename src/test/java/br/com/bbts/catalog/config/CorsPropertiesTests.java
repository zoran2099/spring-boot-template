package br.com.bbts.catalog.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

class CorsPropertiesTests {

	@Test
	void acceptsOnlyExactHttpOrigins() {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = factory.getValidator();

			assertTrue(validator.validate(new CorsProperties(List.of(
					"http://localhost:3000", "https://frontend.example"))).isEmpty());
			assertFalse(validator.validate(new CorsProperties(List.of("*"))).isEmpty());
			assertFalse(validator.validate(new CorsProperties(List.of("https://frontend.example/app"))).isEmpty());
		}
	}

}
