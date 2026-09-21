package org.springframework.samples.petclinic.acl.configuration;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/login", "/css/**", "/h2-console/**").permitAll()
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/records", true)
						.permitAll())
				.logout(logout -> logout
						.logoutSuccessUrl("/login?logout")
						.permitAll())
				.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}

	@Bean
	UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager(
				User.withUsername("ana").password("{noop}demo").roles("VET").build(),
				User.withUsername("bruno").password("{noop}demo").roles("VET").build(),
				User.withUsername("carla").password("{noop}demo").roles("ASSISTANT").build(),
				User.withUsername("japarejo").password("{noop}prueba").roles("VET").build(),
				User.withUsername("admin").password("{noop}admin").roles("ADMIN").build());

	}

	@Bean
	PasswordEncoder passwordEncoder() {

		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
		}

	@Bean
	MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
		DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
		handler.setPermissionEvaluator(permissionEvaluator);
		return handler;
	}

	@Bean
	PermissionEvaluator permissionEvaluator(MutableAclService aclService) {
		return new AclPermissionEvaluator(aclService);
	}

	@Bean
	MutableAclService aclService(DataSource dataSource) {
		AclAuthorizationStrategy authorizationStrategy = new AclAuthorizationStrategyImpl(
				new SimpleGrantedAuthority("ROLE_ADMIN"));
		DefaultPermissionGrantingStrategy grantingStrategy = new DefaultPermissionGrantingStrategy(
				new ConsoleAuditLogger());
		SpringCacheBasedAclCache aclCache = new SpringCacheBasedAclCache(
				new ConcurrentMapCache("aclCache"),
				grantingStrategy,
				authorizationStrategy);
		BasicLookupStrategy lookupStrategy = new BasicLookupStrategy(
				dataSource,
				aclCache,
				authorizationStrategy,
				grantingStrategy);
		return new JdbcMutableAclService(dataSource, lookupStrategy, aclCache);
	}

}
