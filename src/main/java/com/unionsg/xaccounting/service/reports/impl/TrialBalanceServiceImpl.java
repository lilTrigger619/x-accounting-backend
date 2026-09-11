package com.unionsg.xaccounting.service.reports.impl;

import com.unionsg.xaccounting.dto.reports.TrialBalanceResponseDTO;
import com.unionsg.xaccounting.dto.reports.TrialBalanceRowDto;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.NormalBalance;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.projection.ProfitLossAccountProjection;
import com.unionsg.xaccounting.repository.reports.LedgerAsOfBalanceRepository;
import com.unionsg.xaccounting.service.reports.TrialBalanceService;
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
public class TrialBalanceServiceImpl implements TrialBalanceService {

    private static final List<AccountType> ALL_TYPES = List.of(
            AccountType.ASSET, AccountType.LIABILITY, AccountType.EQUITY,
            AccountType.INCOME, AccountType.EXPENSE
    );

    private final LedgerAsOfBalanceRepository ledgerAsOfBalanceRepository;

    @Override
    public TrialBalanceResponseDTO generateReport(LocalDate asOfDate) {

        if (asOfDate == null) {
            throw new BadRequestException("As-of date is required");
        }

        List<ProfitLossAccountProjection> accounts = ledgerAsOfBalanceRepository.findAsOfBalances(asOfDate, ALL_TYPES);

        List<TrialBalanceRowDto> rows = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (ProfitLossAccountProjection account : accounts) {

            BigDecimal balance = CalculateBalance.calculateBalance(
                    account.getTotalDebit(), account.getTotalCredit(), account.getNormalBalance()
            );

            if (balance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            boolean normalSideIsDebit = account.getNormalBalance() == NormalBalance.DEBIT;
            boolean balanceIsPositive = balance.compareTo(BigDecimal.ZERO) > 0;
            // A positive balance sits on the account's own normal side; a negative (contra/abnormal)
            // balance sits on the opposite side, in its absolute-value form.
            boolean showsAsDebit = normalSideIsDebit == balanceIsPositive;

            BigDecimal absBalance = balance.abs();
            BigDecimal debit = showsAsDebit ? absBalance : BigDecimal.ZERO;
            BigDecimal credit = showsAsDebit ? BigDecimal.ZERO : absBalance;

            rows.add(new TrialBalanceRowDto(
                    account.getAccountId(), account.getAccountCode(), account.getAccountName(),
                    account.getAccountType(), debit, credit
            ));

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        return new TrialBalanceResponseDTO(asOfDate, rows, totalDebit, totalCredit, totalDebit.subtract(totalCredit));
    }
}
