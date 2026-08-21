package br.com.bbts.catalog.config;

import java.net.URI;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<@NotBlank String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
	}

	@AssertTrue(message = "must contain only exact HTTP(S) origins without paths, queries or fragments")
	public boolean isAllowedOriginsValid() {
		return allowedOrigins.stream().allMatch(CorsProperties::isValidOrigin);
	}

	private static boolean isValidOrigin(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}

		try {
			URI origin = URI.create(value);
			return ("http".equalsIgnoreCase(origin.getScheme()) || "https".equalsIgnoreCase(origin.getScheme()))
					&& origin.getHost() != null
					&& origin.getUserInfo() == null
					&& (origin.getPath() == null || origin.getPath().isEmpty())
					&& origin.getQuery() == null
					&& origin.getFragment() == null;
		}
		catch (IllegalArgumentException exception) {
			return false;
		}
	}

}
