package com.unionsg.xaccounting.controller;

//import com.yourcompany.accounting.report.dto.ProfitLossReportResponse;
//import com.yourcompany.accounting.report.service.ProfitAndLossService;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportResponseDTO;
import com.unionsg.xaccounting.service.reports.ProfitAndLossService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ProfitAndLossController {

    private final ProfitAndLossService profitAndLossService;

    @GetMapping("/profit-loss")
    public ResponseEntity<ProfitLossReportResponseDTO> getProfitAndLossReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {

        ProfitLossReportResponseDTO response =
                profitAndLossService.generateReport(fromDate, toDate);

        return ResponseEntity.ok(response);
    }

}