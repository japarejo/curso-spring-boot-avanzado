package org.springframework.samples.petclinic.web.api;

import java.util.Comparator;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * MODULO 2 - Gestion centralizada de errores de la API con ProblemDetail (RFC 9457).
 *
 * Importante el `basePackages`: sin el, este @RestControllerAdvice se aplica
 * tambien a los controladores MVC que devuelven JSP, y convierte en un 500 JSON
 * lo que deberia renderizar la pagina de error. Un advice de API se acota a la API.
 */
@RestControllerAdvice(basePackages = "org.springframework.samples.petclinic.web.api")
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		problem.setTitle("Recurso no encontrado");
		problem.setDetail(ex.getMessage());
		return problem;
	}

	@ExceptionHandler({ BadRequestException.class, DuplicatedPetNameException.class,
			MethodArgumentNotValidException.class, ConstraintViolationException.class })
	public ProblemDetail handleBadRequest(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		problem.setTitle("Peticion no valida");
		problem.setDetail(detailFor(ex));
		return problem;
	}

	/**
	 * Sin este metodo, el catch-all de abajo capturaria tambien los fallos de
	 * autorizacion y los convertiria en 500. El cliente debe recibir 403.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		problem.setTitle("Acceso denegado");
		problem.setDetail("No tiene permisos para realizar esta operacion");
		return problem;
	}

	/**
	 * Excepciones que ya llevan su propio estado y cuerpo (ResponseStatusException
	 * y companeras). Se respetan tal cual en lugar de degradarlas a 500.
	 */
	@ExceptionHandler(ErrorResponseException.class)
	public ProblemDetail handleErrorResponse(ErrorResponseException ex) {
		return ex.getBody();
	}

	/**
	 * Red de seguridad. Dos decisiones deliberadas (modulo 3, "errores frecuentes"):
	 *   - el mensaje de la excepcion se REGISTRA pero no se devuelve, para no filtrar
	 *     trazas, nombres de tabla o SQL al cliente;
	 *   - se devuelve un identificador de correlacion para poder localizar el fallo
	 *     en el registro sin exponer nada.
	 */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		String referencia = Long.toHexString(System.nanoTime());
		log.error("Error no controlado en la API [ref={}]", referencia, ex);

		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problem.setTitle("Error interno");
		problem.setDetail("Se ha producido un error inesperado. Referencia: " + referencia);
		return problem;
	}

	private String detailFor(Exception ex) {
		if (ex instanceof MethodArgumentNotValidException validationException) {
			return detailFor(validationException);
		}
		if (ex instanceof ConstraintViolationException constraintViolationException) {
			return detailFor(constraintViolationException);
		}
		return ex.getMessage();
	}

	private String detailFor(MethodArgumentNotValidException ex) {
		return ex.getBindingResult().getFieldErrors().stream()
			.sorted(Comparator.comparing(FieldError::getField))
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining("; "));
	}

	private String detailFor(ConstraintViolationException ex) {
		return ex.getConstraintViolations().stream()
			.sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
			.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
			.collect(Collectors.joining("; "));
	}

}
