package com.unionsg.xaccounting.controller.analytics;

import com.unionsg.xaccounting.dto.analytics.AlertSettingDto;
import com.unionsg.xaccounting.dto.analytics.AnalyticsContextResponse;
import com.unionsg.xaccounting.dto.analytics.AnalyticsFilter;
import com.unionsg.xaccounting.dto.analytics.DrillAccountsResponse;
import com.unionsg.xaccounting.dto.analytics.DrillLinesResponse;
import com.unionsg.xaccounting.dto.analytics.ExecutiveDashboardResponse;
import com.unionsg.xaccounting.dto.analytics.RevenueAnalyticsResponse;
import com.unionsg.xaccounting.dto.analytics.UpdateAlertSettingRequest;
import com.unionsg.xaccounting.service.analytics.AlertSettingsService;
import com.unionsg.xaccounting.service.analytics.AnalyticsContextService;
import com.unionsg.xaccounting.service.analytics.AnalyticsDrillService;
import com.unionsg.xaccounting.service.analytics.ExecutiveDashboardService;
import com.unionsg.xaccounting.service.analytics.RevenueAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Business Intelligence", description = "Management analytics: executive dashboard, revenue analytics, drill-down")
public class AnalyticsController {

    private final AnalyticsContextService contextService;
    private final ExecutiveDashboardService executiveDashboardService;
    private final RevenueAnalyticsService revenueAnalyticsService;
    private final AnalyticsDrillService drillService;
    private final AlertSettingsService alertSettingsService;

    @GetMapping("/context")
    @Operation(summary = "Base currency, current financial year and period, and supported filters")
    public AnalyticsContextResponse context() {
        return contextService.context();
    }

    @GetMapping("/executive")
    @Operation(summary = "Executive dashboard: KPIs with comparison, trends and management alerts")
    public ExecutiveDashboardResponse executive(@ModelAttribute AnalyticsFilter filter) {
        return executiveDashboardService.build(filter);
    }

    @GetMapping("/revenue")
    @Operation(summary = "Revenue analytics: totals, growth, trend and breakdowns")
    public RevenueAnalyticsResponse revenue(@ModelAttribute AnalyticsFilter filter) {
        return revenueAnalyticsService.build(filter);
    }

    @GetMapping("/drill/accounts")
    @Operation(summary = "Drill a figure down to its categories and accounts")
    public DrillAccountsResponse drillAccounts(
            @RequestParam(required = false) List<String> classes,
            @RequestParam(required = false) List<Long> accountIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "PERIOD") String basis,
            @RequestParam(required = false) String label) {
        return drillService.accounts(classes, accountIds, from, to, basis, label);
    }

    @GetMapping("/drill/lines")
    @Operation(summary = "Drill an account down to its journal lines and source transactions")
    public DrillLinesResponse drillLines(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "PERIOD") String basis,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return drillService.lines(accountId, from, to, basis, page, size);
    }

    @GetMapping("/alert-settings")
    @Operation(summary = "Management alert thresholds")
    public List<AlertSettingDto> alertSettings() {
        return alertSettingsService.list();
    }

    @PutMapping("/alert-settings/{key}")
    @Operation(summary = "Change a management alert's threshold, window or on/off switch")
    public AlertSettingDto updateAlertSetting(@PathVariable String key, @RequestBody UpdateAlertSettingRequest request) {
        return alertSettingsService.update(key, request);
    }
}
