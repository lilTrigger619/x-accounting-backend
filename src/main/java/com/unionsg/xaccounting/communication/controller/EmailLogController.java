package com.unionsg.xaccounting.communication.controller;

import com.unionsg.xaccounting.communication.dto.EmailLogResponse;
import com.unionsg.xaccounting.communication.enums.EmailStatus;
import com.unionsg.xaccounting.communication.service.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-logs")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogService emailLogService;

    @GetMapping
    public ResponseEntity<Page<EmailLogResponse>> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) EmailStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(emailLogService.listLogs(entityType, status, pageable));
    }
}
