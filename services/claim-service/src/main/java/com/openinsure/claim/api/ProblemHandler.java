package com.openinsure.claim.api;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProblemHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Problem> handle(ApiException e) {
        return response(e.status().value(), e.status().getReasonPhrase(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Problem> handleUnexpected(Exception e) {
        return response(500, "Internal Server Error", "Unexpected internal error");
    }

    private ResponseEntity<Problem> response(int status, String title, String detail) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new Problem(URI.create("https://openinsure.local/problems/" + status), title, status, detail,
                        UUID.randomUUID()));
    }

    record Problem(URI type, String title, int status, String detail, UUID correlationId) {
    }
}
