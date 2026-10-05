package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.downpayment.DownpaymentAllocationResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentRefundResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.downpayment.Downpayment;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentAllocation;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentRefund;
import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;

public class DownpaymentMapper {

    private DownpaymentMapper() {
        throw new UnsupportedOperationException("Mapper class cannot be instantiated");
    }

    /** Header fields only; used by list screens. */
    public static DownpaymentResponse toSummary(Downpayment dp) {
        DownpaymentResponse r = new DownpaymentResponse();
        r.setId(dp.getId());
        r.setDownpaymentNumber(dp.getDownpaymentNumber());
        r.setType(dp.getType());
        r.setTypeLabel(dp.getType().getLabel());
        r.setCustomerId(dp.getCustomer() != null ? dp.getCustomer().getId() : null);
        r.setSupplierId(dp.getSupplier() != null ? dp.getSupplier().getId() : null);
        r.setCounterpartyId(dp.getCounterpartyId());
        r.setCounterpartyName(dp.getCounterpartyName());
        r.setPaymentDate(dp.getPaymentDate());
        r.setAmount(dp.getAmount());
        r.setCurrency(dp.getCurrency());
        if (dp.getBankAccount() != null) {
            r.setBankAccountId(dp.getBankAccount().getId());
            r.setBankAccountName(dp.getBankAccount().getAccountName());
        }
        if (dp.getControlAccount() != null) {
            r.setControlAccountCode(dp.getControlAccount().getAccountId());
            r.setControlAccountName(dp.getControlAccount().getAccountName());
        }
        r.setReference(dp.getReference());
        r.setDescription(dp.getDescription());
        r.setAppliedAmount(dp.getAppliedAmount());
        r.setRefundedAmount(dp.getRefundedAmount());
        r.setAvailableBalance(dp.getAvailableBalance());
        r.setStatus(dp.getStatus());
        r.setStatusLabel(dp.getStatus().getLabel());
        r.setJournalId(id(dp.getJournal()));
        r.setJournalNumber(number(dp.getJournal()));
        r.setReversalJournalId(id(dp.getReversalJournal()));
        r.setReversalJournalNumber(number(dp.getReversalJournal()));
        r.setPostedAt(dp.getPostedAt());
        r.setCancelledAt(dp.getCancelledAt());
        r.setReversedAt(dp.getReversedAt());
        r.setReversalReason(dp.getReversalReason());
        r.setCreatedAt(dp.getCreatedAt());
        r.setCreatedBy(CreatedByMapper.toDto(dp.getCreatedBy()));
        return r;
    }

    /** Header plus full allocation and refund history; used by the details screen. */
    public static DownpaymentResponse toResponse(Downpayment dp) {
        DownpaymentResponse r = toSummary(dp);
        r.setAllocations(dp.getAllocations().stream().map(DownpaymentMapper::toAllocationResponse).toList());
        r.setRefunds(dp.getRefunds().stream().map(DownpaymentMapper::toRefundResponse).toList());
        return r;
    }

    public static DownpaymentAllocationResponse toAllocationResponse(DownpaymentAllocation a) {
        DownpaymentAllocationResponse r = new DownpaymentAllocationResponse();
        r.setId(a.getId());
        r.setDownpaymentId(a.getDownpayment().getId());
        r.setDownpaymentNumber(a.getDownpayment().getDownpaymentNumber());
        r.setCounterpartyName(a.getDownpayment().getCounterpartyName());
        r.setDocumentType(a.getDocumentType());
        r.setDocumentId(a.getDocumentId());
        r.setDocumentNumber(a.getDocumentNumber());
        if (a.getDocumentType() == SettlementDocumentType.INVOICE && a.getInvoice() != null) {
            r.setDocumentTotal(a.getInvoice().getTotalAmount());
            r.setDocumentBalance(a.getInvoice().getBalance());
        } else if (a.getBill() != null) {
            r.setDocumentTotal(a.getBill().getTotalAmount());
            r.setDocumentBalance(a.getBill().getBalance());
        }
        r.setAmount(a.getAmount());
        r.setAllocationDate(a.getAllocationDate());
        r.setNotes(a.getNotes());
        r.setJournalId(id(a.getJournal()));
        r.setJournalNumber(number(a.getJournal()));
        r.setReversed(a.getReversed());
        r.setReversedAt(a.getReversedAt());
        r.setReversalReason(a.getReversalReason());
        r.setReversalJournalId(id(a.getReversalJournal()));
        r.setReversalJournalNumber(number(a.getReversalJournal()));
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }

    public static DownpaymentRefundResponse toRefundResponse(DownpaymentRefund f) {
        DownpaymentRefundResponse r = new DownpaymentRefundResponse();
        r.setId(f.getId());
        r.setDownpaymentId(f.getDownpayment().getId());
        r.setRefundNumber(f.getRefundNumber());
        r.setAmount(f.getAmount());
        r.setRefundDate(f.getRefundDate());
        if (f.getBankAccount() != null) {
            r.setBankAccountId(f.getBankAccount().getId());
            r.setBankAccountName(f.getBankAccount().getAccountName());
        }
        r.setReference(f.getReference());
        r.setReason(f.getReason());
        r.setJournalId(id(f.getJournal()));
        r.setJournalNumber(number(f.getJournal()));
        r.setReversed(f.getReversed());
        r.setReversedAt(f.getReversedAt());
        r.setReversalReason(f.getReversalReason());
        r.setReversalJournalId(id(f.getReversalJournal()));
        r.setReversalJournalNumber(number(f.getReversalJournal()));
        r.setCreatedAt(f.getCreatedAt());
        return r;
    }

    private static Long id(JournalEntry j) {
        return j != null ? j.getId() : null;
    }

    private static String number(JournalEntry j) {
        return j != null ? j.getJournalNumber() : null;
    }
}
