package com.unionsg.xaccounting.service.dashboard;

import com.unionsg.xaccounting.dto.dashboard.DashboardResponseDTO;

import java.time.LocalDate;

public interface DashboardService {

    DashboardResponseDTO getSummary(LocalDate asOfDate);

}
