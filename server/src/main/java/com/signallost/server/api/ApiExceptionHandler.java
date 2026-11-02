package com.signallost.server.api;

import com.signallost.server.verify.RunRejectedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Turns the two things that go wrong into responses a client can act on. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * The moves do not amount to the run they were submitted as.
     *
     * <p>422 rather than 400: the request was well formed and understood, and
     * the server simply disagrees with what it says. Whether that is a bug in
     * the client or somebody claiming a score they did not earn is not
     * distinguished, because the server cannot tell and does not need to.
     */
    @ExceptionHandler(RunRejectedException.class)
    public ResponseEntity<Map<String, String>> rejected(RunRejectedException rejected) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "error", "run rejected",
                "reason", rejected.getMessage()));
    }

    /** The request did not even have the right shape. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException invalid) {
        Map<String, String> fields = new LinkedHashMap<>();
        invalid.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid request",
                "fields", fields));
    }
}
