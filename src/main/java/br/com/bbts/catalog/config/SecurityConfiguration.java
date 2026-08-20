package br.com.bbts.catalog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BasicAuthProperties.class)
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
						.hasRole("ADMIN")
						.requestMatchers("/actuator/**").hasRole("ADMIN")
						.requestMatchers("/api/v1/**").hasAnyRole("READER", "ADMIN")
						.anyRequest().denyAll())
				.httpBasic(Customizer.withDefaults());

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(BasicAuthProperties properties, PasswordEncoder passwordEncoder) {
		UserDetails reader = User.withUsername(properties.reader().username())
				.password(passwordEncoder.encode(properties.reader().password()))
				.roles("READER")
				.build();

		UserDetails admin = User.withUsername(properties.admin().username())
				.password(passwordEncoder.encode(properties.admin().password()))
				.roles("ADMIN", "READER")
				.build();

		return new InMemoryUserDetailsManager(reader, admin);
	}

}
