package org.springframework.samples.petclinic.web.api;

import java.util.Comparator;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.samples.petclinic.service.exceptions.DuplicatedPetNameException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso no encontrado");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler({ BadRequestException.class, DuplicatedPetNameException.class, MethodArgumentNotValidException.class,
        ConstraintViolationException.class })
    public ProblemDetail handleBadRequest(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Peticion no valida");
        problem.setDetail(detailFor(ex));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Error interno");
        problem.setDetail(ex.getMessage());
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
