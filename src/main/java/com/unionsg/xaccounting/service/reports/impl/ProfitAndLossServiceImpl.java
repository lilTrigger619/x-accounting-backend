package com.unionsg.xaccounting.service.reports.impl;

//import com.yourcompany.accounting.chart.enums.AccountType;
//import com.yourcompany.accounting.chart.enums.NormalBalance;
//import com.yourcompany.accounting.report.dto.ProfitLossAccountDto;
//import com.yourcompany.accounting.report.dto.ProfitLossReportResponse;
//import com.yourcompany.accounting.report.exception.InvalidReportDateException;
//import com.yourcompany.accounting.report.projection.ProfitLossAccountProjection;
//import com.yourcompany.accounting.report.repository.ProfitAndLossRepository;
//import com.yourcompany.accounting.report.service.ProfitAndLossService;
import com.unionsg.xaccounting.dto.reports.ProfitLossAccountDto;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportResponseDTO;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.NormalBalance;
import com.unionsg.xaccounting.projection.ProfitLossAccountProjection;
import com.unionsg.xaccounting.repository.reports.ProfitAndLossRepository;
import com.unionsg.xaccounting.service.reports.ProfitAndLossService;
import com.unionsg.xaccounting.service.reports.exception.InvalidReportDateException;
import com.unionsg.xaccounting.utils.CalculateBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfitAndLossServiceImpl implements ProfitAndLossService {

    private final ProfitAndLossRepository repository;

    @Override
    public ProfitLossReportResponseDTO generateReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        validateDates(fromDate, toDate);

        List<ProfitLossAccountProjection> projections =
                repository.findProfitLossAccounts(
                        fromDate,
                        toDate,
                        List.of(
                                AccountType.INCOME,
                                AccountType.EXPENSE
                        )
                );

        List<ProfitLossAccountDto> revenueAccounts = new ArrayList<>();
        List<ProfitLossAccountDto> expenseAccounts = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (ProfitLossAccountProjection account : projections) {

//            BigDecimal balance = calculateBalance(account);

            BigDecimal balance = CalculateBalance.calculateBalance(
                    account.getTotalDebit(),
                    account.getTotalCredit(),
                    account.getNormalBalance()
            );

            ProfitLossAccountDto dto = new ProfitLossAccountDto(
                    account.getAccountId(),
                    account.getAccountCode(),
                    account.getAccountName(),
                    balance
            );

            if (account.getAccountType() == AccountType.INCOME) {

                revenueAccounts.add(dto);
                totalRevenue = totalRevenue.add(balance);

            } else if (account.getAccountType() == AccountType.EXPENSE) {

                expenseAccounts.add(dto);
                totalExpenses = totalExpenses.add(balance);

            }

        }

        BigDecimal net = totalRevenue.subtract(totalExpenses);

        BigDecimal netProfit = net.max(BigDecimal.ZERO);

        BigDecimal netLoss = net.signum() < 0
                ? net.abs()
                : BigDecimal.ZERO;

        return new ProfitLossReportResponseDTO(

                fromDate,

                toDate,

                List.copyOf(revenueAccounts),

                List.copyOf(expenseAccounts),

                totalRevenue,

                totalExpenses,

                netProfit,

                netLoss

        );

    }

    private BigDecimal calculateBalance(
            ProfitLossAccountProjection account
    ) {

        if (account.getNormalBalance() == NormalBalance.CREDIT) {

            return account.getTotalCredit()
                    .subtract(account.getTotalDebit());

        }

        return account.getTotalDebit()
                .subtract(account.getTotalCredit());

    }

    private void validateDates(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        if (fromDate == null || toDate == null) {
            throw new InvalidReportDateException(
                    "From date and To date are required."
            );
        }

        if (fromDate.isAfter(toDate)) {
            throw new InvalidReportDateException(
                    "From date cannot be after To date."
            );
        }

    }

}