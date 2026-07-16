package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.ReportTemplateDraftLockDto;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateDraftLockService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/report-templates")
@RequiredArgsConstructor
public class ReportTemplateDraftLockController {

    private final ReportTemplateDraftLockService lockService;
    private final com.unionsg.xaccounting.config.DraftLockTtlConfig ttlConfig;



    public record LockRequest(
            @NotBlank
            String lockedBy,
            @NotBlank
            String editSessionId
    ) {
    }



    @PostMapping("/{id}/lock")
    public ResponseEntity<ReportTemplateDraftLockDto> lock(
            @PathVariable("id") Long templateId,
            @RequestBody LockRequest request
    ) {
        return ResponseEntity.ok(lockService.lock(templateId, request.lockedBy(), request.editSessionId(), ttlConfig.getTtlSeconds()));
    }

    @DeleteMapping("/{id}/lock")
    public ResponseEntity<Map<String, Object>> unlock(
            @PathVariable("id") Long templateId,
            @RequestParam String lockedBy,
            @RequestParam String editSessionId
    ) {
        lockService.unlock(templateId, lockedBy, editSessionId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{id}/lock")
    public ResponseEntity<ReportTemplateDraftLockDto> getLock(@PathVariable("id") Long templateId) {
        return ResponseEntity.ok(lockService.get(templateId));
    }
}

