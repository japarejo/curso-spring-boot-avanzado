<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="petclinic" tagdir="/WEB-INF/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<%@ attribute name="name" required="true" rtexprvalue="true"
	description="Nombre del menu activo: home, owners o error"%>

<%--
    MODULO 3 - Seguridad: autorizacion en la capa de vista.

    <sec:authorize access="..."> evalua una expresion de Spring Security y
    muestra u oculta el fragmento. Mensaje clave para clase: esto es
    USABILIDAD, no seguridad. Ocultar un enlace no protege el endpoint;
    la proteccion real esta en SecurityFilterChain y en @PreAuthorize.
    Compruebalo en clase navegando a mano a una URL oculta.
--%>

<nav class="navbar">
	<div class="container">
		<a class="navbar-brand" href="<spring:url value="/" htmlEscape="true"/>">PetClinic</a>

		<ul class="nav">
			<petclinic:menuItem active="${name eq 'home'}" url="/" title="Inicio">Inicio</petclinic:menuItem>
			<petclinic:menuItem active="${name eq 'owners'}" url="/owners/find" title="Buscar propietarios">Propietarios</petclinic:menuItem>
			<li><a href="<spring:url value="/swagger-ui.html" htmlEscape="true"/>">API REST</a></li>
		</ul>

		<ul class="nav nav-right">
			<sec:authorize access="!isAuthenticated()">
				<li><a href="<c:url value="/login"/>">Entrar</a></li>
			</sec:authorize>
			<sec:authorize access="isAuthenticated()">
				<li class="user"><sec:authentication property="name"/></li>
				<li>
					<form action="<c:url value="/logout"/>" method="post">
						<sec:csrfInput/>
						<button type="submit" class="btn btn-link">Salir</button>
					</form>
				</li>
			</sec:authorize>
			<sec:authorize access="hasAuthority('admin')">
				<li class="badge-admin">admin</li>
			</sec:authorize>
		</ul>
	</div>
</nav>
