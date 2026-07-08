package com.unionsg.xaccounting.exception;

import com.unionsg.xaccounting.service.reports.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ReportTemplateControllerAdvice {

    @ExceptionHandler({TemplateNotFoundException.class,
            TemplateSectionAccountNotFoundException.class})
    public ResponseEntity<String> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler({TemplateCodeAlreadyExistsException.class,
            TemplateSectionCodeAlreadyExistsException.class,
            TemplateSectionDisplayOrderConflictException.class,
            TemplateSectionAccountAlreadyAssignedException.class})
    public ResponseEntity<String> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler({TemplatePublishedDeletionException.class,
            TemplateSectionCycleDetectedException.class})
    public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}

