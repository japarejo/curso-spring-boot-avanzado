# syntax=docker/dockerfile:1.7
# =============================================================================
#  MODULO 5 - Imagen de contenedor para cualquier modulo del curso.
#
#  Se construye desde la RAIZ del repositorio:
#    docker build -f docker/Dockerfile --build-arg MODULE=apps/petclinic-api -t petclinic-api .
#
#  Normalmente no hace falta invocarlo a mano: docker/compose.yaml lo usa para
#  los seis servicios pasando MODULE.
#
#  Tres decisiones que merece la pena comentar en clase:
#
#  1. El repositorio local de Maven se monta como CACHE de BuildKit
#     (`--mount=type=cache`). Las dependencias se descargan una vez y se
#     reutilizan en todas las construcciones posteriores, sin quedarse dentro
#     de la imagen final. Es la alternativa moderna a la vieja receta de
#     "copiar los pom.xml y ejecutar dependency:go-offline", que en un reactor
#     de seis modulos tarda una eternidad y ni siquiera resuelve todo.
#
#  2. El contenedor se ejecuta con un usuario sin privilegios. Por defecto
#     corre como root, y no hay ninguna razon para que una aplicacion web lo haga.
#
#  3. El `sed` sobre mvnw no es superfluo: ver el comentario mas abajo.
#
#  Ejercicio del modulo 5: convertir esta imagen en una imagen POR CAPAS con
#      java -Djarmode=tools -jar app.jar extract --layers --launcher
#  y comparar cuanto se retransmite entre dos versiones.
#  Alternativa sin Dockerfile, con buildpacks:
#      ./mvnw -pl apps/petclinic-api spring-boot:build-image
# =============================================================================

# -----------------------------------------------------------------------------
# Etapa 1: compilacion
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /workspace

COPY . .

# El `sed` normaliza los finales de linea de mvnw.
#
# En el repositorio el fichero esta en LF y .gitattributes lo mantiene asi, pero
# `docker build` copia el ARBOL DE TRABAJO, no lo que hay en git. Basta con que
# alguien edite mvnw en Windows con un editor que reintroduzca CRLF para que el
# shebang quede como `#!/bin/sh\r`, el interprete no exista, y la construccion
# falle con el desconcertante
#     /bin/sh: 1: ./mvnw: not found
# Dos lineas de seguro frente a un fallo que cuesta media hora diagnosticar.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    sed -i 's/\r$//' ./mvnw && chmod +x ./mvnw \
    && ./mvnw -B -pl ${MODULE} -am clean package -DskipTests \
    && mkdir -p /artefacto \
    && origen="$(find ${MODULE}/target -maxdepth 1 \( -name '*.jar' -o -name '*.war' \) ! -name '*.original' | head -n 1)" \
    && cp "${origen}" "/artefacto/app.${origen##*.}"

# Se CONSERVA la extension original (.jar o .war), y no es un detalle cosmetico.
#
# petclinic-api se empaqueta como war por las JSP. Si el fichero se renombra a
# algo sin extension conocida, Tomcat deja de reconocerlo como archivo web:
# arranca igual, la API REST responde, pero CUALQUIER vista JSP devuelve 404.
# El fallo es silencioso y desconcertante, porque /actuator/health dice UP.

# -----------------------------------------------------------------------------
# Etapa 2: imagen final. Solo el artefacto y una JRE.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

# petclinic-api se empaqueta como war (por las JSP) y el resto como jar.
# Los dos son ejecutables con `java -jar`.
COPY --from=build --chown=spring:spring /artefacto/ /app/

USER spring

EXPOSE 8080
# Forma de shell con `exec` para poder expandir el comodin: el fichero se llama
# app.jar o app.war segun el modulo. `exec` hace que la JVM sea el proceso 1 del
# contenedor y reciba las senales de parada, en lugar de quedar colgando de un sh.
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.*"]
