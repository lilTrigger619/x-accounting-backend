package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ReportDateValidationControllerAdvice {


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        // Keep consistent with existing ApiResponse format
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .content(null)
                        .build());
    }



    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex) {
        // fallback - avoid leaking internals
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder()
                        .success(false)
                        .message("Unexpected error occurred")
                        .content(null)
                        .build());
    }
}

