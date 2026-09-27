package com.openinsure.consent.api;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class ProblemHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Problem> handleApi(ApiException exception) {
        return response(exception.status(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Problem> handleUnexpected(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error");
    }

    private ResponseEntity<Problem> response(HttpStatus status, String detail) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new Problem(URI.create("https://openinsure.local/problems/" + status.value()),
                        status.getReasonPhrase(), status.value(), detail, UUID.randomUUID()));
    }

    record Problem(URI type, String title, int status, String detail, UUID correlationId) {
    }
}
