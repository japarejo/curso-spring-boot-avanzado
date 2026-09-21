// =============================================================================
// Modulo 3 - Ejercicio 6: petclinic-api como servidor de recursos OAuth 2.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
//
// La dependencia spring-boot-starter-oauth2-resource-server ya esta en el
// pom.xml de apps/petclinic-api; solo falta configurarla.
// =============================================================================
package org.springframework.samples.petclinic.configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Base64;

/**
 * DOS cadenas de filtros, no una.
 *
 * La razon es que hay dos politicas distintas en la misma aplicacion:
 *
 *   /api/**   API sin estado. Se autentica con un JWT en la cabecera
 *             Authorization. No hay sesion, luego no hay cookie de sesion,
 *             luego CSRF no aplica.
 *
 *   resto     Aplicacion web con formulario y sesion. CSRF SI aplica.
 *
 * El @Order es imprescindible: Spring evalua las cadenas en orden y usa la
 * PRIMERA cuyo securityMatcher encaje. Si la cadena web fuera antes, se
 * quedaria con todas las peticiones y la de la API no se usaria nunca.
 */
@Configuration
public class ApiResourceServerConfiguration {

    /**
     * Debe ser EL MISMO secreto que usa auth-service para firmar.
     *
     * Y aqui esta precisamente el limite de la firma simetrica: petclinic-api
     * necesita el secreto para VALIDAR, y con ese secreto tambien podria EMITIR
     * tokens. Con RSA bastaria con la clave publica. Ver el ejercicio 5.
     */
    @Value("${jwt.secret}")
    private String secretoBase64;

    @Bean
    @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                // Las lecturas siguen siendo publicas para poder explorar la
                // API en clase; las escrituras exigen token.
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeAutoridades())))
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        byte[] bytes = java.util.Base64.getDecoder().decode(secretoBase64);
        SecretKey clave = new SecretKeySpec(bytes, "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(clave)
            .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS512)
            .build();
    }

    /**
     * Por defecto Spring Security busca las autoridades en el claim `scope` o
     * `scp` y les antepone "SCOPE_". Nuestro auth-service las emite en un claim
     * llamado `authorities` y sin prefijo, asi que hay que decirselo.
     *
     * Es el fallo mas habitual al montar un servidor de recursos: el token es
     * valido, la peticion se autentica, y aun asi devuelve 403 porque las
     * autoridades no son las que esperan los matchers.
     */
    private JwtAuthenticationConverter conversorDeAutoridades() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("authorities");
        autoridades.setAuthorityPrefix("");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}

// ---------- En SecurityConfiguration, la cadena web pasa a @Order(2) ----------
/*
    @Bean
    @Order(2)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception { ... }
*/

// ---------- Comprobacion ----------
/*
  # 1. Levantar auth-service (puerto 8060) y petclinic-api (8080)
  # 2. Pedir un token
  TOKEN=$(curl -s -X POST localhost:8060/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin1","password":"4dm1n"}' | jq -r .token)

  # 3. Sin token: la lectura pasa, la escritura no
  curl -i localhost:8080/api/v1/visits                      # 200
  curl -i -X DELETE localhost:8080/api/pets/1               # 401

  # 4. Con token
  curl -i -X DELETE localhost:8080/api/pets/1 \
    -H "Authorization: Bearer $TOKEN"                       # 200

  # 5. Con un token manipulado
  curl -i localhost:8080/api/pets -X POST \
    -H "Authorization: Bearer ${TOKEN}X"                    # 401
*/
