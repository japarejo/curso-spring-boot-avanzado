// =============================================================================
// Modulo 3 - Ejercicio 12: formulario de login propio.
//
// SOLUCION DE REFERENCIA. No forma parte de la compilacion.
// =============================================================================

// ---------- web/LoginController.java ----------
package org.springframework.samples.petclinic.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Solo sirve la vista. El POST /login NO se programa: lo atiende el
 * UsernamePasswordAuthenticationFilter de Spring Security antes de que la
 * peticion llegue a ningun controlador.
 */
@Controller
public class LoginController {

	@GetMapping("/login")
	public String login() {
		return "login";
	}

}

// ---------- WEB-INF/jsp/login.jsp ----------
/*
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="petclinic" tagdir="/WEB-INF/tags" %>

<petclinic:layout pageName="login">
    <h2>Acceso a PetClinic</h2>

    <c:if test="${param.error != null}">
        <div class="alert alert-danger">Usuario o contrasena incorrectos.</div>
    </c:if>
    <c:if test="${param.logout != null}">
        <div class="alert alert-info">Has cerrado la sesion.</div>
    </c:if>

    <form method="post" action="<c:url value='/login'/>">
        <label for="username">Usuario</label>
        <input type="text" id="username" name="username" class="form-control" required autofocus/>

        <label for="password">Contrasena</label>
        <input type="password" id="password" name="password" class="form-control" required/>

        <label><input type="checkbox" name="remember-me"/> Recordarme</label>

        <!-- Sin esta linea, el POST /login da 403 -->
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

        <button type="submit" class="btn btn-primary">Entrar</button>
    </form>
</petclinic:layout>
*/
// Si el tag layout no encaja, sirve igual una pagina HTML minima: lo que se
// evalua es el formulario, no el diseno.

// ---------- SecurityConfiguration: sustituye formLogin y logout ----------
/*
			.formLogin(form -> form
				.loginPage("/login")              // GET: nuestra vista. POST: el filtro.
				.failureUrl("/login?error")
				.defaultSuccessUrl("/welcome")
				.permitAll())                     // /login tiene que ser publico
			.logout(logout -> logout
				.logoutSuccessUrl("/login?logout")
				.deleteCookies("JSESSIONID")
				.permitAll())
			.rememberMe(remember -> remember
				// La clave firma la cookie. Tiene que ser secreta y estable: si cambia
				// en cada arranque, todas las cookies "recordarme" dejan de valer.
				.key("clave-del-curso-cambiar")   // en produccion, leida de una propiedad o secreto
				.tokenValiditySeconds(7 * 24 * 3600))
			.sessionManagement(session -> session
				// Una sola sesion por usuario: la segunda entrada expulsa a la primera.
				.maximumSessions(1))
*/
// rememberMe() necesita un UserDetailsService: usa el UserDetailsManager que ya
// declara SecurityConfiguration.
//
// Nota: con la cadena actual, "/login" no aparece en las reglas de autorizacion
// y la regla final es denyAll(). formLogin(...).permitAll() lo abre por nosotros.
//
// Cookie "remember-me": base64(usuario:caducidad:algoritmo:firma). Se puede
// decodificar con `echo <valor> | base64 -d` para verlo en clase.
