package com.andesstay.reservations.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidStatusTransitionException.class)
	public ResponseEntity<ApiError> handleInvalidTransition(InvalidStatusTransitionException ex) {
		return new ResponseEntity<>(new ApiError(HttpStatus.CONFLICT.value(), "Estado invalido", ex.getMessage()),
				HttpStatus.CONFLICT);
	}

	@ExceptionHandler(ReservationNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ReservationNotFoundException ex) {
		return new ResponseEntity<>(new ApiError(HttpStatus.NOT_FOUND.value(), "No encontrado", ex.getMessage()),
				HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
		return new ResponseEntity<>(new ApiError(HttpStatus.BAD_REQUEST.value(), "Solicitud invalida", ex.getMessage()),
				HttpStatus.BAD_REQUEST);
	}

	public record ApiError(int status, String error, String message) {
	}
}