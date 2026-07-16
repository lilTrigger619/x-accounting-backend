package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.ReportTemplateHistoryDto;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
@RequestMapping("/api/v1/report-templates")
@RequiredArgsConstructor
public class ReportTemplateHistoryController {

    private final ReportTemplateHistoryService historyService;

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ReportTemplateHistoryDto>> getHistory(@PathVariable("id") Long templateId) {
        return ResponseEntity.ok(historyService.getHistory(templateId));
    }
}

