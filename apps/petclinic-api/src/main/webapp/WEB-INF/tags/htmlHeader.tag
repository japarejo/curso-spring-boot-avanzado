<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%--
    Cabecera HTML comun.

    Curso Spring Boot avanzado: esta vista es deliberadamente minima.
    No usa webjars (jQuery, jQuery-UI, Bootstrap, CKEditor) ni CSS generado
    por wro4j/LESS: una unica hoja de estilos estatica servida desde
    src/main/resources/static/resources/css/app.css.

    Motivo: el temario evalua APIs REST y seguridad, no la capa de vista.
    Estas pocas JSP existen solo para poder demostrar en vivo formLogin
    y <sec:authorize> (modulo 3).
--%>

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <spring:url value="/resources/images/favicon.png" var="favicon"/>
    <link rel="shortcut icon" type="image/x-icon" href="${favicon}">

    <title>PetClinic :: Curso Spring Boot avanzado</title>

    <spring:url value="/resources/css/app.css" var="appCss"/>
    <link href="${appCss}" rel="stylesheet"/>
</head>
