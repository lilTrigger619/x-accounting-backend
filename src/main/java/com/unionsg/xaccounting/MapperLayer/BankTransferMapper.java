package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.banking.BankTransferAccountSummary;
import com.unionsg.xaccounting.dto.banking.BankTransferAccountingLine;
import com.unionsg.xaccounting.dto.banking.BankTransferActivityResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferListItemResponse;
import com.unionsg.xaccounting.dto.banking.BankTransferResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.banking.BankTransfer;
import com.unionsg.xaccounting.entity.banking.BankTransferActivity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BankTransferMapper {

    private final AccountRepository accountRepository;

    public BankTransferAccountSummary toAccountSummary(BankAccount account, BigDecimal availableBalance) {
        if (account == null) {
            return null;
        }
        return BankTransferAccountSummary.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .bankName(account.getBankName())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .glAccountCode(account.getGlAccountCode())
                .glAccountName(accountRepository.findByAccountId(account.getGlAccountCode())
                        .map(AccountEntity::getAccountName).orElse(null))
                .status(account.getStatus() != null ? account.getStatus().name() : null)
                .isDefault(Boolean.TRUE.equals(account.getIsDefault()))
                .allowOverdraft(Boolean.TRUE.equals(account.getAllowOverdraft()))
                .availableBalance(availableBalance)
                .build();
    }

    public BankTransferListItemResponse toListItem(BankTransfer t) {
        return BankTransferListItemResponse.builder()
                .id(t.getId())
                .transferNumber(t.getTransferNumber())
                .reference(t.getReference())
                .transferDate(t.getTransferDate())
                .valueDate(t.getValueDate())
                .sourceAccount(toAccountSummary(t.getSourceBankAccount(), null))
                .destinationAccount(toAccountSummary(t.getDestinationBankAccount(), null))
                .amount(t.getAmount())
                .sourceCurrency(t.getSourceCurrency())
                .convertedAmount(t.getConvertedAmount())
                .destinationCurrency(t.getDestinationCurrency())
                .feeAmount(t.getFeeAmount())
                .status(t.getStatus())
                .createdBy(CreatedByMapper.toDto(t.getCreatedBy()))
                .createdAt(t.getCreatedAt())
                .build();
    }

    public BankTransferResponse toResponse(BankTransfer t, List<BankTransferAccountingLine> accountingLines,
                                           String accountingPreviewError) {
        return BankTransferResponse.builder()
                .id(t.getId())
                .transferNumber(t.getTransferNumber())
                .reference(t.getReference())
                .description(t.getDescription())
                .status(t.getStatus())
                .sourceAccount(toAccountSummary(t.getSourceBankAccount(), null))
                .destinationAccount(toAccountSummary(t.getDestinationBankAccount(), null))
                .transferDate(t.getTransferDate())
                .valueDate(t.getValueDate())
                .amount(t.getAmount())
                .sourceCurrency(t.getSourceCurrency())
                .destinationCurrency(t.getDestinationCurrency())
                .exchangeRate(t.getExchangeRate())
                .convertedAmount(t.getConvertedAmount())
                .feeAmount(t.getFeeAmount())
                .feeAccountCode(t.getFeeAccount() != null ? t.getFeeAccount().getAccountId() : null)
                .feeAccountName(t.getFeeAccount() != null ? t.getFeeAccount().getAccountName() : null)
                .baseCurrency(t.getBaseCurrency())
                .sourceBaseRate(t.getSourceBaseRate())
                .destinationBaseRate(t.getDestinationBaseRate())
                .baseAmount(t.getBaseAmount())
                .baseConvertedAmount(t.getBaseConvertedAmount())
                .baseFeeAmount(t.getBaseFeeAmount())
                .exchangeGainLoss(t.getExchangeGainLoss())
                .accountingLines(accountingLines)
                .accountingPreviewError(accountingPreviewError)
                .journalId(t.getJournal() != null ? t.getJournal().getId() : null)
                .journalNumber(t.getJournal() != null ? t.getJournal().getJournalNumber() : null)
                .reversalJournalId(t.getReversalJournal() != null ? t.getReversalJournal().getId() : null)
                .reversalJournalNumber(t.getReversalJournal() != null ? t.getReversalJournal().getJournalNumber() : null)
                .createdBy(CreatedByMapper.toDto(t.getCreatedBy()))
                .createdAt(t.getCreatedAt())
                .updatedBy(t.getUpdatedBy())
                .updatedAt(t.getUpdatedAt())
                .postedAt(t.getPostedAt())
                .postedBy(t.getPostedBy())
                .reversedAt(t.getReversedAt())
                .reversedBy(t.getReversedBy())
                .reversalReason(t.getReversalReason())
                .cancelledAt(t.getCancelledAt())
                .cancelledBy(t.getCancelledBy())
                .cancellationReason(t.getCancellationReason())
                .version(t.getVersion())
                .build();
    }

    public BankTransferAccountingLine toAccountingLine(JournalLine line) {
        return BankTransferAccountingLine.builder()
                .accountCode(line.getAccount().getAccountId())
                .accountName(line.getAccount().getAccountName())
                .description(line.getDescription())
                .debit(line.getDebitAmount())
                .credit(line.getCreditAmount())
                .build();
    }

    public BankTransferActivityResponse toActivity(BankTransferActivity a) {
        return BankTransferActivityResponse.builder()
                .id(a.getId())
                .action(a.getAction())
                .fromStatus(a.getFromStatus())
                .toStatus(a.getToStatus())
                .details(a.getDetails())
                .performedBy(CreatedByMapper.toDto(a.getCreatedBy()))
                .performedAt(a.getCreatedAt())
                .build();
    }
}
