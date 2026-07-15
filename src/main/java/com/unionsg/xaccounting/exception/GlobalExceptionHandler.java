package com.unionsg.xaccounting.exception;

import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.service.reports.exception.InvalidReportDateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .content(null)
                        .build()
                );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
//        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .content(null)
                        .build()
                );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .content(null)
                        .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(errors)
                        .content(null)
                        .build()
                );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntime(RuntimeException ex) {
        System.out.println("RuntimeException: " + ex.getMessage());

        ex.printStackTrace();
        String message = ex.getMessage();

        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (message != null && message.toLowerCase().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (message != null && message.toLowerCase().contains("already exists")) {
            status = HttpStatus.CONFLICT;
        } else if (message != null && message.toLowerCase().contains("invalid credentials")) {
            status = HttpStatus.UNAUTHORIZED;
        }

        return ResponseEntity.status(status)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(message)
                        .content(null)
                        .build()
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        System.out.println("Unhandled exception: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder()
                        .success(false)
                        .message("An unexpected error occurredd")
                        .content(null)
                        .build()
                );
    }

//
//    @ExceptionHandler(InvalidReportDateException.class)
//    public ResponseEntity<?> handleInvalidReportDate(
//            InvalidReportDateException ex
//    ) {
//
//        return buildResponse(
//                HttpStatus.BAD_REQUEST,
//                ex.getMessage()
//        );
//    }
//
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<?> handleGenericException(
//            Exception ex
//    ) {
//
//        return buildResponse(
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                "Something went wrong while generating report"
//        );
//    }
//

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message
    ) {

        Map<String, Object> body = new HashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return new ResponseEntity<>(body, status);
    }



}