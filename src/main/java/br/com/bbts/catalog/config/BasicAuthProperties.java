package br.com.bbts.catalog.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record BasicAuthProperties(@Valid User reader, @Valid User admin) {

	public BasicAuthProperties {
		if (reader == null || admin == null) {
			throw new IllegalArgumentException("Reader and admin credentials must be configured");
		}
	}

	public record User(@NotBlank String username, @NotBlank String password) {
	}

}
