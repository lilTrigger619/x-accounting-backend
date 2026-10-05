package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.deposit.DepositAllocationResponse;
import com.unionsg.xaccounting.dto.deposit.DepositListItemResponse;
import com.unionsg.xaccounting.dto.deposit.DepositResponse;
import com.unionsg.xaccounting.dto.deposit.DepositTypeResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.deposit.Deposit;
import com.unionsg.xaccounting.entity.deposit.DepositAllocation;
import com.unionsg.xaccounting.entity.deposit.DepositType;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.deposit.DepositClassification;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import com.unionsg.xaccounting.enums.deposit.DepositStatus;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.service.deposit.DepositJournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class DepositMapper {

    private final DepositJournalService journalService;
    private final AccountRepository accountRepository;

    public static DepositTypeResponse toTypeResponse(DepositType type) {
        DepositTypeResponse response = new DepositTypeResponse();
        response.setId(type.getId());
        response.setName(type.getName());
        response.setDescription(type.getDescription());
        response.setDirection(type.getDirection());
        response.setDirectionLabel(type.getDirection() != null ? type.getDirection().getLabel() : null);
        response.setRefundableByDefault(type.getRefundableByDefault());
        response.setInterestBearingByDefault(type.getInterestBearingByDefault());
        AccountEntity account = type.getDepositAccount();
        if (account != null) {
            response.setDepositAccountId(account.getId());
            response.setDepositAccountCode(account.getAccountId());
            response.setDepositAccountName(account.getAccountName());
        }
        AccountEntity forfeit = type.getForfeitureAccount();
        if (forfeit != null) {
            response.setForfeitureAccountId(forfeit.getId());
            response.setForfeitureAccountCode(forfeit.getAccountId());
            response.setForfeitureAccountName(forfeit.getAccountName());
        }
        response.setActive(type.getActive());
        response.setCreatedAt(type.getCreatedAt());
        response.setUpdatedAt(type.getUpdatedAt());
        return response;
    }

    /**
     * Balance-sheet placement: paid deposits are assets, received deposits liabilities; either is
     * non-current when it is not expected back within 12 months of the deposit date.
     */
    public static DepositClassification classify(Deposit deposit) {
        boolean nonCurrent = deposit.getExpectedReturnDate() != null && deposit.getDepositDate() != null
                && deposit.getExpectedReturnDate().isAfter(deposit.getDepositDate().plusMonths(12));
        if (deposit.getDirection() == DepositDirection.DEPOSIT_PAID) {
            return nonCurrent ? DepositClassification.NON_CURRENT_ASSET : DepositClassification.CURRENT_ASSET;
        }
        return nonCurrent ? DepositClassification.NON_CURRENT_LIABILITY : DepositClassification.CURRENT_LIABILITY;
    }

    public static String accountingTreatment(Deposit deposit) {
        boolean refundable = Boolean.TRUE.equals(deposit.getRefundable());
        if (deposit.getDirection() == DepositDirection.DEPOSIT_PAID) {
            return refundable
                    ? "Held as an asset (a refundable deposit the counterparty owes back) until it is returned, applied to a supplier bill or forfeited. Paying it is not an expense; only a forfeiture is."
                    : "Held as an asset (an advance to the counterparty) until it is applied to a supplier bill or forfeited. Paying it is not an expense; only a forfeiture is.";
        }
        return refundable
                ? "Held as a liability (a refundable deposit owed back to the counterparty) until it is refunded, applied to an invoice or forfeited. Receiving it is not income; only a forfeiture is."
                : "Held as a liability (a customer advance) until it is applied to an invoice or forfeited. Receiving it is not income; only a forfeiture is.";
    }

    public DepositResponse toResponse(Deposit deposit) {
        DepositResponse r = new DepositResponse();
        r.setId(deposit.getId());
        r.setDepositNumber(deposit.getDepositNumber());
        r.setDirection(deposit.getDirection());
        r.setDirectionLabel(deposit.getDirection().getLabel());
        if (deposit.getDepositType() != null) {
            r.setDepositTypeId(deposit.getDepositType().getId());
            r.setDepositTypeName(deposit.getDepositType().getName());
        }
        r.setCounterpartyType(deposit.getCounterpartyType());
        r.setCounterpartyTypeLabel(deposit.getCounterpartyType().getLabel());
        r.setCounterpartyName(deposit.getCounterpartyName());
        r.setCustomerId(deposit.getCustomer() != null ? deposit.getCustomer().getId() : null);
        r.setSupplierId(deposit.getSupplier() != null ? deposit.getSupplier().getId() : null);
        r.setEmployeeId(deposit.getEmployee() != null ? deposit.getEmployee().getId() : null);
        r.setAmount(deposit.getAmount());
        r.setCurrency(deposit.getCurrency());
        r.setDepositDate(deposit.getDepositDate());
        r.setExpectedReturnDate(deposit.getExpectedReturnDate());
        r.setRefundable(deposit.getRefundable());
        r.setPurpose(deposit.getPurpose());
        r.setReference(deposit.getReference());
        if (deposit.getBankAccount() != null) {
            r.setBankAccountId(deposit.getBankAccount().getId());
            r.setBankAccountName(bankName(deposit.getBankAccount()));
        }
        r.setDepositAccountId(deposit.getDepositAccount() != null ? deposit.getDepositAccount().getId() : null);
        try {
            String code = journalService.depositAccountCode(deposit);
            r.setEffectiveAccountCode(code);
            accountRepository.findByAccountId(code).ifPresent(a -> r.setEffectiveAccountName(a.getAccountName()));
        } catch (RuntimeException ignored) {
            // A missing mapping is reported when the deposit posts; the detail view still loads.
        }
        DepositClassification classification = classify(deposit);
        r.setClassification(classification);
        r.setClassificationLabel(classification.getLabel());
        r.setAccountingTreatment(accountingTreatment(deposit));
        r.setInterestBearing(deposit.getInterestBearing());
        r.setInterestRate(deposit.getInterestRate());
        r.setInterestStartDate(deposit.getInterestStartDate());
        r.setInterestTerms(deposit.getInterestTerms());
        r.setEstimatedInterest(estimatedInterest(deposit));
        r.setNotes(deposit.getNotes());
        r.setStatus(deposit.getStatus());
        r.setStatusLabel(deposit.getStatus().getLabel());
        r.setAppliedAmount(deposit.getAppliedAmount());
        r.setRefundedAmount(deposit.getRefundedAmount());
        r.setForfeitedAmount(deposit.getForfeitedAmount());
        r.setTransferredAmount(deposit.getTransferredAmount());
        r.setAvailableBalance(deposit.getAvailableBalance());
        if (deposit.getJournal() != null) {
            r.setJournalId(deposit.getJournal().getId());
            r.setJournalNumber(deposit.getJournal().getJournalNumber());
        }
        if (deposit.getTransferredFrom() != null) {
            r.setTransferredFromId(deposit.getTransferredFrom().getId());
            r.setTransferredFromNumber(deposit.getTransferredFrom().getDepositNumber());
        }
        r.setActivatedAt(deposit.getActivatedAt());
        r.setCancelledAt(deposit.getCancelledAt());
        r.setReversedAt(deposit.getReversedAt());
        r.setReversalReason(deposit.getReversalReason());
        r.setCreatedAt(deposit.getCreatedAt());
        r.setUpdatedAt(deposit.getUpdatedAt());
        deposit.getAllocations().forEach(a -> r.getAllocations().add(toAllocationResponse(a)));
        return r;
    }

    public static DepositListItemResponse toListItem(Deposit deposit) {
        DepositListItemResponse r = new DepositListItemResponse();
        r.setId(deposit.getId());
        r.setDepositNumber(deposit.getDepositNumber());
        r.setDirection(deposit.getDirection());
        r.setDirectionLabel(deposit.getDirection().getLabel());
        if (deposit.getDepositType() != null) {
            r.setDepositTypeId(deposit.getDepositType().getId());
            r.setDepositTypeName(deposit.getDepositType().getName());
        }
        r.setCounterpartyType(deposit.getCounterpartyType());
        r.setCounterpartyName(deposit.getCounterpartyName());
        r.setCustomerId(deposit.getCustomer() != null ? deposit.getCustomer().getId() : null);
        r.setSupplierId(deposit.getSupplier() != null ? deposit.getSupplier().getId() : null);
        r.setEmployeeId(deposit.getEmployee() != null ? deposit.getEmployee().getId() : null);
        r.setAmount(deposit.getAmount());
        r.setCurrency(deposit.getCurrency());
        r.setDepositDate(deposit.getDepositDate());
        r.setExpectedReturnDate(deposit.getExpectedReturnDate());
        r.setRefundable(deposit.getRefundable());
        r.setReference(deposit.getReference());
        r.setStatus(deposit.getStatus());
        r.setStatusLabel(deposit.getStatus().getLabel());
        DepositClassification classification = classify(deposit);
        r.setClassification(classification);
        r.setClassificationLabel(classification.getLabel());
        r.setAppliedAmount(deposit.getAppliedAmount());
        r.setRefundedAmount(deposit.getRefundedAmount());
        r.setForfeitedAmount(deposit.getForfeitedAmount());
        r.setTransferredAmount(deposit.getTransferredAmount());
        r.setAvailableBalance(deposit.getAvailableBalance());
        r.setCreatedAt(deposit.getCreatedAt());
        return r;
    }

    public static DepositAllocationResponse toAllocationResponse(DepositAllocation a) {
        DepositAllocationResponse r = new DepositAllocationResponse();
        Deposit deposit = a.getDeposit();
        r.setId(a.getId());
        r.setDepositId(deposit.getId());
        r.setDepositNumber(deposit.getDepositNumber());
        r.setDirection(deposit.getDirection());
        r.setCounterpartyName(deposit.getCounterpartyName());
        r.setCurrency(deposit.getCurrency());
        r.setAllocationType(a.getAllocationType());
        r.setAllocationTypeLabel(a.getAllocationType().getLabel());
        r.setAmount(a.getAmount());
        r.setAllocationDate(a.getAllocationDate());
        if (a.getInvoice() != null) {
            r.setInvoiceId(a.getInvoice().getId());
            r.setInvoiceNumber(a.getInvoice().getInvoiceNumber());
        }
        if (a.getBill() != null) {
            r.setBillId(a.getBill().getId());
            r.setBillNumber(a.getBill().getBillNumber());
        }
        if (a.getTargetDeposit() != null) {
            r.setTargetDepositId(a.getTargetDeposit().getId());
            r.setTargetDepositNumber(a.getTargetDeposit().getDepositNumber());
        }
        if (a.getBankAccount() != null) {
            r.setBankAccountId(a.getBankAccount().getId());
            r.setBankAccountName(bankName(a.getBankAccount()));
        }
        r.setReference(a.getReference());
        r.setNotes(a.getNotes());
        if (a.getJournal() != null) {
            r.setJournalId(a.getJournal().getId());
            r.setJournalNumber(a.getJournal().getJournalNumber());
        }
        r.setReversed(a.getReversed());
        r.setReversedAt(a.getReversedAt());
        r.setReversalReason(a.getReversalReason());
        if (a.getReversalJournal() != null) {
            r.setReversalJournalId(a.getReversalJournal().getId());
            r.setReversalJournalNumber(a.getReversalJournal().getJournalNumber());
        }
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }

    /** Simple interest on the available balance from the interest start date to today. */
    public static BigDecimal estimatedInterest(Deposit deposit) {
        if (!Boolean.TRUE.equals(deposit.getInterestBearing()) || deposit.getInterestRate() == null
                || deposit.getStatus() == DepositStatus.DRAFT || deposit.getStatus() == DepositStatus.CANCELLED
                || deposit.getStatus() == DepositStatus.REVERSED) {
            return null;
        }
        LocalDate start = deposit.getInterestStartDate() != null ? deposit.getInterestStartDate() : deposit.getDepositDate();
        long days = ChronoUnit.DAYS.between(start, LocalDate.now());
        if (days <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return deposit.getAvailableBalance()
                .multiply(deposit.getInterestRate())
                .multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(36500), 2, RoundingMode.HALF_UP);
    }

    private static String bankName(BankAccount bank) {
        String name = bank.getAccountName() != null ? bank.getAccountName() : bank.getBankName();
        return bank.getAccountNumber() != null ? name + " (" + bank.getAccountNumber() + ")" : name;
    }
}
