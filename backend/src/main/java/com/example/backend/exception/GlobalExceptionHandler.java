package com.example.backend.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, Object>> handleApiException(ApiException exception) {
		return ResponseEntity.status(exception.getStatus()).body(Map.of(
				"timestamp", Instant.now(),
				"status", exception.getStatus().value(),
				"error", exception.getStatus().getReasonPhrase(),
				"message", exception.getMessage()));
	}
}
