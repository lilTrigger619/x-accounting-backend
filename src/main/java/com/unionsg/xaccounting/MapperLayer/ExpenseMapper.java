package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.expense.ExpenseAccountingLine;
import com.unionsg.xaccounting.dto.expense.ExpenseActivityResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseLineResponse;
import com.unionsg.xaccounting.dto.expense.ExpenseListItemResponse;
import com.unionsg.xaccounting.dto.expense.ExpensePaymentAccount;
import com.unionsg.xaccounting.dto.expense.ExpenseResponse;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.expense.Expense;
import com.unionsg.xaccounting.entity.expense.ExpenseActivity;
import com.unionsg.xaccounting.entity.expense.ExpenseLine;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExpenseMapper {

    private final UserRepository userRepository;

    public ExpensePaymentAccount toPaymentAccount(BankAccount account) {
        if (account == null) {
            return null;
        }
        return ExpensePaymentAccount.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .bankName(account.getBankName())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .glAccountCode(account.getGlAccountCode())
                .build();
    }

    public ExpenseListItemResponse toListItem(Expense e) {
        return ExpenseListItemResponse.builder()
                .id(e.getId())
                .expenseNumber(e.getExpenseNumber())
                .reference(e.getReference())
                .paymentDate(e.getPaymentDate())
                .supplierId(e.getSupplier() != null ? e.getSupplier().getId() : null)
                .supplierName(e.getSupplier() != null ? e.getSupplier().getDisplayName() : null)
                .paymentAccount(toPaymentAccount(e.getPaymentAccount()))
                .paymentMethod(e.getPaymentMethod())
                .currency(e.getCurrency())
                .totalAmount(e.getTotalAmount())
                .lineCount(e.getLines().size())
                .status(e.getStatus())
                .createdBy(CreatedByMapper.toDto(e.getCreatedBy()))
                .createdAt(e.getCreatedAt())
                .build();
    }

    public ExpenseResponse toResponse(Expense e, List<ExpenseAccountingLine> accountingLines, String accountingPreviewError) {
        return ExpenseResponse.builder()
                .id(e.getId())
                .expenseNumber(e.getExpenseNumber())
                .reference(e.getReference())
                .status(e.getStatus())
                .supplierId(e.getSupplier() != null ? e.getSupplier().getId() : null)
                .supplierName(e.getSupplier() != null ? e.getSupplier().getDisplayName() : null)
                .supplierCode(e.getSupplier() != null ? e.getSupplier().getSupplierCode() : null)
                .paymentAccount(toPaymentAccount(e.getPaymentAccount()))
                .paymentDate(e.getPaymentDate())
                .paymentMethod(e.getPaymentMethod())
                .currency(e.getCurrency())
                .baseCurrency(e.getBaseCurrency())
                .exchangeRate(e.getExchangeRate())
                .totalAmount(e.getTotalAmount())
                .baseTotalAmount(e.getBaseTotalAmount())
                .memo(e.getMemo())
                .lines(e.getLines().stream().map(this::toLine).toList())
                .accountingLines(accountingLines)
                .accountingPreviewError(accountingPreviewError)
                .journalId(e.getJournal() != null ? e.getJournal().getId() : null)
                .journalNumber(e.getJournal() != null ? e.getJournal().getJournalNumber() : null)
                .reversalJournalId(e.getReversalJournal() != null ? e.getReversalJournal().getId() : null)
                .reversalJournalNumber(e.getReversalJournal() != null ? e.getReversalJournal().getJournalNumber() : null)
                .createdBy(CreatedByMapper.toDto(e.getCreatedBy()))
                .createdAt(e.getCreatedAt())
                .updatedBy(userName(e.getUpdatedBy()))
                .updatedAt(e.getUpdatedAt())
                .postedAt(e.getPostedAt())
                .postedBy(e.getPostedBy())
                .reversedAt(e.getReversedAt())
                .reversedBy(e.getReversedBy())
                .reversalReason(e.getReversalReason())
                .version(e.getVersion())
                .build();
    }

    public ExpenseLineResponse toLine(ExpenseLine line) {
        return ExpenseLineResponse.builder()
                .id(line.getId())
                .lineNumber(line.getLineNumber())
                .expenseDate(line.getExpenseDate())
                .category(line.getCategory())
                .accountId(line.getAccount().getId())
                .accountCode(line.getAccount().getAccountId())
                .accountName(line.getAccount().getAccountName())
                .description(line.getDescription())
                .amount(line.getAmount())
                .baseAmount(line.getBaseAmount())
                .build();
    }

    public ExpenseAccountingLine toAccountingLine(JournalLine line) {
        return ExpenseAccountingLine.builder()
                .accountCode(line.getAccount().getAccountId())
                .accountName(line.getAccount().getAccountName())
                .description(line.getDescription())
                .debit(line.getDebitAmount())
                .credit(line.getCreditAmount())
                .build();
    }

    public ExpenseActivityResponse toActivity(ExpenseActivity a) {
        return ExpenseActivityResponse.builder()
                .id(a.getId())
                .action(a.getAction())
                .fromStatus(a.getFromStatus())
                .toStatus(a.getToStatus())
                .details(a.getDetails())
                .performedBy(CreatedByMapper.toDto(a.getCreatedBy()))
                .performedAt(a.getCreatedAt())
                .build();
    }

    /** BaseEntity stores the last editor as a user id; show their name instead. */
    private String userName(String userId) {
        if (userId == null) {
            return null;
        }
        try {
            return userRepository.findById(UUID.fromString(userId))
                    .map(u -> CreatedByMapper.toDto(u).getFullName())
                    .orElse(userId);
        } catch (IllegalArgumentException notAUuid) {
            return userId;
        }
    }
}
