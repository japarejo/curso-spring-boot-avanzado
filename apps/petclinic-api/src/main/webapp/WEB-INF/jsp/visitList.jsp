<%@ page session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="petclinic" tagdir="/WEB-INF/tags" %>

<%--
    Historial de visitas de una mascota.

    Esta vista NO existia en el proyecto anterior, pese a que
    VisitController.showVisits la devolvia: GET /owners/*/pets/{petId}/visits
    terminaba en un error 500.

    MODULO 4 - Merece la pena ensenarlo: VisitControllerTests.testShowVisits
    estaba en verde igualmente, porque @WebMvcTest comprueba el NOMBRE de la
    vista pero no la renderiza. Una rebanada web no sustituye a una prueba de
    integracion que atraviese la capa de vista.
--%>

<petclinic:layout pageName="owners">
    <h2>Historial de visitas</h2>

    <table class="table table-striped table-condensed">
        <thead>
        <tr>
            <th style="width: 120px;">Fecha</th>
            <th>Descripcion</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${visits}" var="visit">
            <tr>
                <td><c:out value="${visit.date}"/></td>
                <td><c:out value="${visit.description}"/></td>
            </tr>
        </c:forEach>
        <c:if test="${empty visits}">
            <tr>
                <td colspan="2" class="empty">Esta mascota no tiene visitas registradas.</td>
            </tr>
        </c:if>
        </tbody>
    </table>
</petclinic:layout>
