package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.ReportVersionDetailDto;
import com.unionsg.xaccounting.dto.reports.ReportVersionDto;
import com.unionsg.xaccounting.dto.reports.RollbackResponseDto;
import com.unionsg.xaccounting.service.reports.template.VersionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/report-templates")
@RequiredArgsConstructor
public class VersionHistoryController {

    private final VersionHistoryService versionHistoryService;

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ReportVersionDto>> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(versionHistoryService.getVersions(id));
    }

    @GetMapping("/version/{versionId}")
    public ResponseEntity<ReportVersionDetailDto> getVersion(@PathVariable Long versionId) {
        return ResponseEntity.ok(versionHistoryService.getVersion(versionId));
    }

    @PostMapping("/version/{versionId}/rollback")
    public ResponseEntity<RollbackResponseDto> rollback(
            @PathVariable Long versionId,
            @RequestParam String updatedBy
    ) {
        return ResponseEntity.ok(versionHistoryService.rollback(versionId, updatedBy));
    }
}

