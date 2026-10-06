package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.loan.LoanAccrualResponse;
import com.unionsg.xaccounting.dto.loan.LoanAuditLogResponse;
import com.unionsg.xaccounting.dto.loan.LoanLineResponse;
import com.unionsg.xaccounting.dto.loan.LoanListItemResponse;
import com.unionsg.xaccounting.dto.loan.LoanRepaymentResponse;
import com.unionsg.xaccounting.dto.loan.LoanResponse;
import com.unionsg.xaccounting.dto.loan.LoanTypeResponse;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import com.unionsg.xaccounting.entity.loan.LoanAuditLog;
import com.unionsg.xaccounting.entity.loan.LoanInterestAccrual;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.entity.loan.LoanType;
import com.unionsg.xaccounting.enums.loan.LoanInstallmentStatus;
import com.unionsg.xaccounting.enums.loan.LoanPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LoanMapper {

    private LoanMapper() {
        throw new UnsupportedOperationException("Mapper class cannot be instantiated");
    }

    public static LoanTypeResponse toTypeResponse(LoanType type) {
        LoanTypeResponse response = new LoanTypeResponse();
        response.setId(type.getId());
        response.setName(type.getName());
        response.setDescription(type.getDescription());
        response.setDefaultDirection(type.getDefaultDirection());
        response.setActive(type.getActive());
        response.setCreatedAt(type.getCreatedAt());
        response.setUpdatedAt(type.getUpdatedAt());
        return response;
    }

    /** Status as shown: OVERDUE or MISSED once an unpaid installment's due date has passed. */
    public static LoanInstallmentStatus effectiveStatus(LoanAmortizationLine line, LocalDate today) {
        if (line.getStatus() == LoanInstallmentStatus.PAID) {
            return LoanInstallmentStatus.PAID;
        }
        if (line.isMissed()) {
            return LoanInstallmentStatus.MISSED;
        }
        if (line.getDueDate().isBefore(today)) {
            return LoanInstallmentStatus.OVERDUE;
        }
        return line.getStatus();
    }

    public static LoanLineResponse toLineResponse(LoanAmortizationLine line) {
        return toLineResponse(line, LocalDate.now(), true);
    }

    /** {@code live} is false for a loan that is not running (draft, cancelled, reversed): no overdue flags. */
    public static LoanLineResponse toLineResponse(LoanAmortizationLine line, LocalDate today, boolean live) {
        LoanLineResponse response = new LoanLineResponse();
        response.setId(line.getId());
        response.setInstallmentNumber(line.getInstallmentNumber());
        response.setDueDate(line.getDueDate());
        response.setOpeningPrincipal(line.getOpeningPrincipal());
        response.setPrincipalDue(line.getPrincipalDue());
        response.setInterestDue(line.getInterestDue());
        response.setFeesDue(line.getFeesDue());
        response.setTotalInstallment(line.getTotalInstallment());
        response.setClosingPrincipal(line.getClosingPrincipal());
        response.setPrincipalPaid(line.getPrincipalPaid());
        response.setInterestPaid(line.getInterestPaid());
        response.setFeesPaid(line.getFeesPaid());
        response.setAmountOutstanding(line.totalOwed());
        LoanInstallmentStatus status = live ? effectiveStatus(line, today) : line.getStatus();
        response.setStatus(status);
        response.setMissed(line.isMissed());
        response.setMissedNote(line.getMissedNote());
        if (live && (status == LoanInstallmentStatus.OVERDUE || status == LoanInstallmentStatus.MISSED)) {
            response.setDaysOverdue(Math.max(0, ChronoUnit.DAYS.between(line.getDueDate(), today)));
        }
        return response;
    }

    public static LoanResponse toResponse(Loan loan) {
        LocalDate today = LocalDate.now();
        boolean live = isLive(loan);
        LoanResponse r = new LoanResponse();
        r.setId(loan.getId());
        r.setLoanNumber(loan.getLoanNumber());
        if (loan.getLoanType() != null) {
            r.setLoanTypeId(loan.getLoanType().getId());
            r.setLoanTypeName(loan.getLoanType().getName());
        }
        r.setDirection(loan.getDirection());
        r.setCounterpartyType(loan.getCounterpartyType());
        r.setCounterpartyName(loan.getCounterpartyName());
        r.setEmployeeId(loan.getEmployee() != null ? loan.getEmployee().getId() : null);
        r.setCustomerId(loan.getCustomer() != null ? loan.getCustomer().getId() : null);
        r.setSupplierId(loan.getSupplier() != null ? loan.getSupplier().getId() : null);
        r.setPrincipalAmount(loan.getPrincipalAmount());
        r.setCurrency(loan.getCurrency());
        r.setInterestRate(loan.getInterestRate());
        r.setInterestMethod(loan.getInterestMethod());
        r.setStartDate(loan.getStartDate());
        r.setMaturityDate(loan.getMaturityDate());
        r.setPaymentFrequency(loan.getPaymentFrequency());
        r.setNumberOfInstallments(loan.getNumberOfInstallments());
        r.setGracePeriodInstallments(loan.getGracePeriodInstallments());
        r.setTotalFees(loan.getTotalFees());
        r.setFeeTreatment(loan.getFeeTreatment());
        r.setInstallmentFee(loan.getInstallmentFee());
        r.setAllowOverpayment(loan.isOverpaymentAllowed());
        if (loan.getBankAccount() != null) {
            r.setBankAccountId(loan.getBankAccount().getId());
            r.setBankAccountName(loan.getBankAccount().getAccountName());
        }
        if (loan.getPrincipalAccount() != null) {
            r.setPrincipalAccountId(loan.getPrincipalAccount().getId());
            r.setPrincipalAccountCode(loan.getPrincipalAccount().getAccountId());
            r.setPrincipalAccountName(loan.getPrincipalAccount().getAccountName());
        }
        if (loan.getInterestAccount() != null) {
            r.setInterestAccountId(loan.getInterestAccount().getId());
            r.setInterestAccountCode(loan.getInterestAccount().getAccountId());
            r.setInterestAccountName(loan.getInterestAccount().getAccountName());
        }
        r.setCollateralDescription(loan.getCollateralDescription());
        r.setCollateralValue(loan.getCollateralValue());
        r.setExternalReference(loan.getExternalReference());
        r.setNotes(loan.getNotes());
        r.setStatus(loan.getStatus());
        r.setStatusReason(loan.getStatusReason());

        List<LoanAmortizationLine> schedule = loan.getSchedule();
        r.setOutstandingPrincipal(loan.getOutstandingPrincipal());
        r.setOutstandingInterest(loan.getOutstandingInterest());
        r.setInterestDueToDate(sum(schedule, l -> !l.getDueDate().isAfter(today), LoanAmortizationLine::interestOwed));
        r.setFeesDueToDate(sum(schedule, l -> !l.getDueDate().isAfter(today), LoanAmortizationLine::feesOwed));
        r.setOverdueAmount(live ? sum(schedule, l -> l.getDueDate().isBefore(today), LoanAmortizationLine::totalOwed) : BigDecimal.ZERO);
        r.setTotalOutstanding(live ? sum(schedule, l -> true, LoanAmortizationLine::totalOwed) : BigDecimal.ZERO);
        r.setPrincipalPaid(sum(schedule, l -> true, LoanAmortizationLine::getPrincipalPaid));
        r.setInterestPaid(sum(schedule, l -> true, LoanAmortizationLine::getInterestPaid));
        r.setFeesPaid(sum(schedule, l -> true, LoanAmortizationLine::getFeesPaid));
        r.setTotalScheduledInterest(sum(schedule, l -> true, LoanAmortizationLine::getInterestDue));
        r.setOverpaymentBalance(loan.getOverpaymentBalance());
        r.setWrittenOffAmount(loan.getWrittenOffAmount());
        schedule.stream().filter(l -> l.totalOwed().signum() > 0).findFirst().ifPresent(next -> {
            r.setNextDueDate(next.getDueDate());
            r.setNextDueAmount(next.totalOwed());
        });

        if (loan.getJournal() != null) {
            r.setJournalId(loan.getJournal().getId());
            r.setJournalNumber(loan.getJournal().getJournalNumber());
        }
        if (loan.getWriteOffJournal() != null) {
            r.setWriteOffJournalId(loan.getWriteOffJournal().getId());
            r.setWriteOffJournalNumber(loan.getWriteOffJournal().getJournalNumber());
        }
        r.setApprovedAt(loan.getApprovedAt());
        r.setDisbursedAt(loan.getDisbursedAt());
        r.setDefaultedAt(loan.getDefaultedAt());
        r.setClosedAt(loan.getClosedAt());
        r.setCancelledAt(loan.getCancelledAt());
        r.setReversedAt(loan.getReversedAt());
        r.setCreatedAt(loan.getCreatedAt());
        r.setUpdatedAt(loan.getUpdatedAt());
        r.setSchedule(schedule.stream().map(l -> toLineResponse(l, today, live)).toList());
        return r;
    }

    public static LoanListItemResponse toListItemResponse(Loan loan) {
        LocalDate today = LocalDate.now();
        LoanListItemResponse r = new LoanListItemResponse();
        r.setId(loan.getId());
        r.setLoanNumber(loan.getLoanNumber());
        if (loan.getLoanType() != null) {
            r.setLoanTypeId(loan.getLoanType().getId());
            r.setLoanTypeName(loan.getLoanType().getName());
        }
        r.setDirection(loan.getDirection());
        r.setCounterpartyName(loan.getCounterpartyName());
        r.setPrincipalAmount(loan.getPrincipalAmount());
        r.setOutstandingPrincipal(loan.getOutstandingPrincipal());
        r.setInterestRate(loan.getInterestRate());
        r.setInterestMethod(loan.getInterestMethod());
        r.setCurrency(loan.getCurrency());
        r.setStartDate(loan.getStartDate());
        r.setMaturityDate(loan.getMaturityDate());
        r.setStatus(loan.getStatus());
        if (isLive(loan)) {
            loan.getSchedule().stream().filter(l -> l.totalOwed().signum() > 0).findFirst().ifPresent(next -> {
                r.setNextDueDate(next.getDueDate());
                r.setNextDueAmount(next.totalOwed());
            });
            r.setOverdueAmount(sum(loan.getSchedule(), l -> l.getDueDate().isBefore(today), LoanAmortizationLine::totalOwed));
        } else {
            r.setOverdueAmount(BigDecimal.ZERO);
        }
        return r;
    }

    public static LoanRepaymentResponse toRepaymentResponse(LoanRepayment p) {
        LoanRepaymentResponse r = new LoanRepaymentResponse();
        r.setId(p.getId());
        r.setRepaymentDate(p.getRepaymentDate());
        r.setPrincipalAmount(p.getPrincipalAmount());
        r.setInterestAmount(p.getInterestAmount());
        r.setFeesAmount(p.getFeesAmount());
        r.setOverpaymentAmount(p.getOverpaymentAmount());
        r.setTotalAmount(p.getTotalAmount());
        r.setPaymentType(p.getPaymentType());
        r.setStatus(p.isPosted() ? LoanPaymentStatus.POSTED : LoanPaymentStatus.REVERSED);
        r.setPaymentMethod(p.getPaymentMethod());
        if (p.getBankAccount() != null) {
            r.setBankAccountId(p.getBankAccount().getId());
            r.setBankAccountName(p.getBankAccount().getAccountName());
        }
        r.setReferenceNumber(p.getReferenceNumber());
        r.setMemo(p.getMemo());
        if (p.getJournal() != null) {
            r.setJournalId(p.getJournal().getId());
            r.setJournalNumber(p.getJournal().getJournalNumber());
        }
        if (p.getReversalJournal() != null) {
            r.setReversalJournalId(p.getReversalJournal().getId());
            r.setReversalJournalNumber(p.getReversalJournal().getJournalNumber());
        }
        r.setReversedAt(p.getReversedAt());
        r.setReversalReason(p.getReversalReason());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }

    public static List<LoanRepaymentResponse> toRepaymentResponses(List<LoanRepayment> repayments) {
        return repayments.stream().map(LoanMapper::toRepaymentResponse).toList();
    }

    public static LoanAccrualResponse toAccrualResponse(LoanInterestAccrual a) {
        LoanAccrualResponse r = new LoanAccrualResponse();
        r.setId(a.getId());
        r.setAccrualDate(a.getAccrualDate());
        r.setAmount(a.getAmount());
        r.setMemo(a.getMemo());
        if (a.getJournal() != null) {
            r.setJournalId(a.getJournal().getId());
            r.setJournalNumber(a.getJournal().getJournalNumber());
        }
        r.setReversed(Boolean.TRUE.equals(a.getReversed()));
        return r;
    }

    public static LoanAuditLogResponse toAuditResponse(LoanAuditLog log) {
        LoanAuditLogResponse r = new LoanAuditLogResponse();
        r.setId(log.getId());
        r.setAction(log.getAction());
        r.setPreviousStatus(log.getPreviousStatus());
        r.setNewStatus(log.getNewStatus());
        r.setDetails(log.getDetails());
        r.setPerformedAt(log.getPerformedAt());
        User user = log.getPerformedBy();
        if (user != null) {
            String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                    + (user.getLastName() != null ? user.getLastName() : "")).trim();
            r.setPerformedBy(name.isEmpty() ? user.getEmail() : name);
        }
        return r;
    }

    /** A loan whose schedule is running: overdue flags and outstanding totals apply. */
    public static boolean isLive(Loan loan) {
        return switch (loan.getStatus()) {
            case ACTIVE, PARTIALLY_PAID, DEFAULTED -> true;
            default -> false;
        };
    }

    private static BigDecimal sum(List<LoanAmortizationLine> lines,
                                  java.util.function.Predicate<LoanAmortizationLine> include,
                                  java.util.function.Function<LoanAmortizationLine, BigDecimal> value) {
        return lines.stream().filter(include).map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
