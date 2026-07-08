package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.ReportSectionAccountBulkRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportSectionAccountListResponseDto;
import com.unionsg.xaccounting.dto.reports.ReportSectionAccountRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportSectionAccountResponseDto;
import com.unionsg.xaccounting.service.reports.ReportSectionAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/report-sections/{reportSectionId}/accounts")
@RequiredArgsConstructor
public class ReportSectionAccountController {

    private final ReportSectionAccountService service;

    @PostMapping
    public ResponseEntity<ReportSectionAccountResponseDto> assignAccount(
            @PathVariable Long reportSectionId,
            @Valid @RequestBody ReportSectionAccountRequestDto request
    ) {
        ReportSectionAccountResponseDto response = service.assignAccount(
                new ReportSectionAccountRequestDto(
                        reportSectionId,
                        request.accountId(),
                        request.displayOrder()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable Long reportSectionId,
            @PathVariable Long accountId
    ) {
        service.removeAssignment(reportSectionId, accountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ReportSectionAccountListResponseDto> listAssignments(
            @PathVariable Long reportSectionId
    ) {
        return ResponseEntity.ok(service.listAssignments(reportSectionId));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<ReportSectionAccountResponseDto>> bulkAssign(
            @PathVariable Long reportSectionId,
            @Valid @RequestBody ReportSectionAccountBulkRequestDto request
    ) {
        List<ReportSectionAccountResponseDto> created = service.bulkAssign(
                new ReportSectionAccountBulkRequestDto(reportSectionId, request.accounts())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

