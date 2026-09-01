package com.unionsg.xaccounting.controller.reports;

import com.unionsg.xaccounting.dto.reports.AgingReportResponseDto;
import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.service.reports.AgingReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Aging Reports", description = "Accounts Receivable and Accounts Payable aging")
public class AgingController {

    private final AgingReportService agingReportService;

    @GetMapping("/ar-aging")
    @Operation(summary = "Accounts Receivable aging", description = "Customer balances bucketed by current / 1-30 / 31-60 / 61-90 / 90+ days overdue")
    public ResponseEntity<ApiResponse<AgingReportResponseDto>> arAging(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        return ResponseEntity.ok(
                ApiResponse.<AgingReportResponseDto>builder()
                        .success(true)
                        .message("AR aging retrieved successfully")
                        .content(agingReportService.getArAging(asOfDate))
                        .build()
        );
    }

    @GetMapping("/ap-aging")
    @Operation(summary = "Accounts Payable aging", description = "Supplier balances bucketed by current / 1-30 / 31-60 / 61-90 / 90+ days overdue")
    public ResponseEntity<ApiResponse<AgingReportResponseDto>> apAging(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        return ResponseEntity.ok(
                ApiResponse.<AgingReportResponseDto>builder()
                        .success(true)
                        .message("AP aging retrieved successfully")
                        .content(agingReportService.getApAging(asOfDate))
                        .build()
        );
    }
}
