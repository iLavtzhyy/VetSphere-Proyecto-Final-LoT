package com.VetSphere.Backend.exception;

/**
 * La cuenta esta bloqueada temporalmente (intentos fallidos) o de forma
 * manual por un administrador. Se traduce a HTTP 423 (Locked) con el
 * envoltorio estandar ApiResponse.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
