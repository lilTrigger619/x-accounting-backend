package com.unionsg.xaccounting.service.reports;

import com.unionsg.xaccounting.dto.reports.TrialBalanceResponseDTO;

import java.time.LocalDate;

public interface TrialBalanceService {

    TrialBalanceResponseDTO generateReport(LocalDate asOfDate);

}
