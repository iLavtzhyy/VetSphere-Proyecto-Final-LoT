package com.VetSphere.Backend.exception;

import com.VetSphere.Backend.shared.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones: TODA respuesta de error conserva el
 * envoltorio ApiResponse { ok, mensaje, data }.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> negocio(Exception ex) {
        return ApiResponse.error(ex.getMessage());
    }

    /** Cuenta bloqueada: HTTP 423 (Locked) para que el frontend lo distinga. */
    @ExceptionHandler(AccountLockedException.class)
    @ResponseStatus(HttpStatus.LOCKED)
    public ApiResponse<Void> bloqueada(AccountLockedException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> noEncontrado(ResourceNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validacion(MethodArgumentNotValidException ex) {
        String campos = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describirCampo)
                .collect(Collectors.joining("; "));
        return ApiResponse.error("Datos invalidos: " + (campos.isBlank() ? "revisa los campos enviados" : campos));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El parametro '" + ex.getName() + "' tiene un valor invalido";
        if (ex.getRequiredType() != null) {
            mensaje += " (se esperaba " + ex.getRequiredType().getSimpleName() + ")";
        }
        return ApiResponse.error(mensaje);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> metodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        String permitidos = ex.getSupportedHttpMethods() == null ? "" :
                " Metodos permitidos: " + String.join(", ", ex.getSupportedHttpMethods().stream().map(metodo -> metodo.name()).toList());
        return ApiResponse.error("Metodo HTTP no soportado: " + ex.getMethod() + "." + permitidos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return ApiResponse.error("Cuerpo de la solicitud invalido o no interpretable.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> noEncontrado(NoResourceFoundException ex) {
        return ApiResponse.error("Recurso no encontrado: " + ex.getResourcePath());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> parametroFaltante(MissingServletRequestParameterException ex) {
        return ApiResponse.error("Falta el parametro obligatorio: " + ex.getParameterName());
    }

    /**
     * Violacion de integridad (codigo de producto duplicado, FK inexistente,
     * columna NOT NULL vacia). Antes terminaba en un 500 generico; ahora se
     * responde 409 con un mensaje util.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> conflictoDatos(DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return ApiResponse.error("La operacion viola una restriccion de datos: el registro ya existe o hace referencia a datos inexistentes.");
    }

    /** Ultima linea de defensa: 500 sin stacktrace ni detalles internos. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> inesperado(Exception ex) {
        log.error("Error no controlado: {}", ex.getMessage(), ex);
        return ApiResponse.error("Error interno del servidor. Intenta nuevamente o contacta al administrador.");
    }

    private String describirCampo(FieldError error) {
        return error.getField() + ": " + (error.getDefaultMessage() == null ? "invalido" : error.getDefaultMessage());
    }
}
