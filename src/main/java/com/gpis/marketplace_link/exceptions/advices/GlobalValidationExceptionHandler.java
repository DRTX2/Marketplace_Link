package com.gpis.marketplace_link.exceptions.advices;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.gpis.marketplace_link.exceptions.business.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.*;

@RestControllerAdvice
@Order(0) // alta prioridad
public class GlobalValidationExceptionHandler {

    // Maneja errores de validación de DTOs (por ejemplo @Valid en el body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleDTOValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Error de validación en campos"
        );
        pd.setTitle("Validation Error");

        Map<String, String> errors = new HashMap<>();
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        for (FieldError fe : fieldErrors) {
            // Si ya hay error en ese campo, no sobreescribir
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        pd.setProperty("errors", errors);
        return pd;
    }

    // Maneja validaciones en parámetros del método (por ejemplo con @Validated sobre parámetros)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodParamValidation(HandlerMethodValidationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Parámetros inválidos"
        );
        pd.setTitle("Parameter Validation Error");

        Map<String, String> errors = new HashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(err -> {
                String msg = err.getDefaultMessage();
                if (!errors.containsKey(name)) {
                    errors.put(name, msg);
                }
            });
        });
        pd.setProperty("errors", errors);
        return pd;
    }

    // Maneja violaciones de constraints directamente (ej: validar con Validator en servicios)
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Violación de restricciones"
        );
        pd.setTitle("Constraint Violation");

        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            String path = cv.getPropertyPath().toString();
            String msg = cv.getMessage();
            // si ya existe un mensaje para ese path, no reemplace
            errors.putIfAbsent(path, msg);
        });
        pd.setProperty("errors", errors);
        return pd;
    }

    // Maneja JSON mal formado / parseo incorrecto
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonInvalid(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "JSON inválido o mal formado"
        );
        pd.setTitle("Malformed JSON");

        Map<String, String> errors = new HashMap<>();
        if (ex.getCause() instanceof JsonMappingException jme) {
            // construir la ruta del campo que falló
            String path = jme.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .reduce((a, b) -> a + "." + b)
                    .orElse("$");
            String msg = jme.getOriginalMessage();
            errors.put(path, msg);
        } else {
            String msg = ex.getMostSpecificCause().getMessage();
            errors.put("$", msg);
        }
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        pd.setTitle("Business Error");
        return pd;
    }
}
