package com.unionsg.xaccounting.controller.accounting;

import com.unionsg.xaccounting.dto.accounting.PeriodActionRequest;
import com.unionsg.xaccounting.dto.accounting.YearEndClosingPreviewResponse;
import com.unionsg.xaccounting.dto.accounting.YearEndClosingResultResponse;
import com.unionsg.xaccounting.service.accounting.YearEndClosingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/financial-years/{financialYearId}/closing")
@RequiredArgsConstructor
public class YearEndClosingController {

    private final YearEndClosingService yearEndClosingService;

    @GetMapping("/preview")
    public ResponseEntity<YearEndClosingPreviewResponse> preview(@PathVariable Long financialYearId) {
        return ResponseEntity.ok(yearEndClosingService.getClosingPreview(financialYearId));
    }

    @PostMapping("/close")
    public ResponseEntity<YearEndClosingResultResponse> close(@PathVariable Long financialYearId) {
        return ResponseEntity.ok(yearEndClosingService.closeFinancialYear(financialYearId));
    }

    @PostMapping("/reopen")
    public ResponseEntity<YearEndClosingResultResponse> reopen(
            @PathVariable Long financialYearId,
            @RequestBody PeriodActionRequest request
    ) {
        return ResponseEntity.ok(yearEndClosingService.reopenFinancialYear(financialYearId, request.getReason()));
    }
}
