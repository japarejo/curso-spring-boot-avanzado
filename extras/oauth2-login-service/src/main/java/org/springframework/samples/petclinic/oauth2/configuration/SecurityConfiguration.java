package org.springframework.samples.petclinic.oauth2.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.samples.petclinic.oauth2.service.InternalUserDetailsService;
import org.springframework.samples.petclinic.oauth2.util.JwtRequestFilter;
import org.springframework.samples.petclinic.oauth2.util.LocalJwtSuccessHandler;
import org.springframework.samples.petclinic.oauth2.util.OAuth2JwtSuccessHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

	private final JwtRequestFilter jwtRequestFilter;
	private final OAuth2JwtSuccessHandler oauth2JwtSuccessHandler;
	private final LocalJwtSuccessHandler localJwtSuccessHandler;

	public SecurityConfiguration(
			JwtRequestFilter jwtRequestFilter,
			OAuth2JwtSuccessHandler oauth2JwtSuccessHandler,
			LocalJwtSuccessHandler localJwtSuccessHandler) {
		this.jwtRequestFilter = jwtRequestFilter;
		this.oauth2JwtSuccessHandler = oauth2JwtSuccessHandler;
		this.localJwtSuccessHandler = localJwtSuccessHandler;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/authenticate",
								"/validate",
								"/refresh",
								"/oauth2/**",
								"/login",
								"/login/oauth2/**",
								"/error",
								"/",
								"/doc",
								"/doc/swagger-config",
								"/swagger*",
								"/swagger-ui/**",
								"/h2-console/**")
						.permitAll()
						.requestMatchers("/api/**").authenticated()
						.requestMatchers("/service-instances/*").authenticated()
						.requestMatchers("/actuator/**").authenticated()
						.anyRequest().denyAll())
				.oauth2Login(oauth2 -> oauth2
						.loginPage("/login")
						.successHandler(oauth2JwtSuccessHandler)
						.failureHandler(new SimpleUrlAuthenticationFailureHandler("/login?error")))
				.formLogin(form -> form
						.loginPage("/login")
						.successHandler(localJwtSuccessHandler)
						.failureHandler(new SimpleUrlAuthenticationFailureHandler("/login?error"))
						.permitAll())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

		return http.build();
	}

	@Bean
	AuthenticationManager authenticationManager(
			InternalUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return NoOpPasswordEncoder.getInstance();
	}

	@Bean
	public RoleHierarchy roleHierarchy() {
		RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
		String hierarchy = "ROLE_ADMIN > ROLE_STAFF \n ROLE_STAFF > ROLE_USER";
		roleHierarchy.setHierarchy(hierarchy);
		return roleHierarchy;
	}


}
