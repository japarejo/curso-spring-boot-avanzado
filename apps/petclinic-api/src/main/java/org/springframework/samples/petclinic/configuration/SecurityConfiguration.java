package org.springframework.samples.petclinic.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.DispatcherType;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author japarejo
 */
@Configuration
@EnableWebSecurity              // opcional; Boot lo activa si detecta spring-security
@EnableMethodSecurity
public class SecurityConfiguration {


    @Autowired
    DataSource dataSource;

    // 1.  AUTORIZACIÓN + FILTROS + CSRF/HEADERS ----------------------------------------
    @Bean
    SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http)
            throws Exception {

        http
            /*---------------- AUTORIZAR PETICIONES ----------------*/
            .authorizeHttpRequests(auth -> auth
            	.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR)
                    .permitAll()
                .requestMatchers("/","/resources/**", "/webjars/**", "/h2-console/**","/welcome","/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/oups").permitAll()
                .requestMatchers(HttpMethod.GET, "/vets", "/diseases").permitAll()
                .requestMatchers("/users/new").permitAll()
                .requestMatchers("/logging", "/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/admin/**").hasAuthority("admin")
                .requestMatchers("/owners/**").hasAnyAuthority("owner", "admin")
                .requestMatchers("/vets/**", "/diseases/**", "/payments/**", "/bills/**").authenticated()
                .requestMatchers("/api/**", "/hystrix**").permitAll()
                .anyRequest().denyAll()
            )

            /*---------------- SESIONES ----------------
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )*/

            /*---------------- FORM LOGIN / LOGOUT ----------------*/
            .formLogin(form -> form                        
                    .defaultSuccessUrl("/welcome")
                    .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/")
            )

            /*---------------- CSRF: ignorar rutas concretas ----------------*/
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/h2-console/**",
                    "/actuator/**",
                    "/api/**"
                )
            )

            /*---------------- HEADERS: permitir frames en H2 console --------*/
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    // 2.  AUTENTICACIÓN JDBC -----------------------------------------------------------
    @Bean
    public UserDetailsManager users(DataSource dataSource) {
        JdbcUserDetailsManager mgr = new JdbcUserDetailsManager(dataSource);
        mgr.setUsersByUsernameQuery(
            "select username, password, enabled from users where username = ?");
        mgr.setAuthoritiesByUsernameQuery(
            "select username, authority from authorities where username = ?");
        return mgr;
    }

    // 3.  PASSWORD ENCODER -------------------------------------------------------------
    @Bean
    PasswordEncoder passwordEncoder() {
        //  ¡NoOp solo para entornos de demo/docencia!
        //return NoOpPasswordEncoder.getInstance();
        return new BCryptPasswordEncoder();
    }
}


