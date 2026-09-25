package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.loan.LoanLineResponse;
import com.unionsg.xaccounting.dto.loan.LoanListItemResponse;
import com.unionsg.xaccounting.dto.loan.LoanRepaymentResponse;
import com.unionsg.xaccounting.dto.loan.LoanResponse;
import com.unionsg.xaccounting.dto.loan.LoanTypeResponse;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.entity.loan.LoanType;

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
        return response;
    }

    public static LoanLineResponse toLineResponse(LoanAmortizationLine line) {
        LoanLineResponse response = new LoanLineResponse();
        response.setId(line.getId());
        response.setInstallmentNumber(line.getInstallmentNumber());
        response.setDueDate(line.getDueDate());
        response.setOpeningPrincipal(line.getOpeningPrincipal());
        response.setPrincipalDue(line.getPrincipalDue());
        response.setInterestDue(line.getInterestDue());
        response.setTotalInstallment(line.getTotalInstallment());
        response.setClosingPrincipal(line.getClosingPrincipal());
        response.setPrincipalPaid(line.getPrincipalPaid());
        response.setInterestPaid(line.getInterestPaid());
        response.setStatus(line.getStatus());
        return response;
    }

    public static LoanResponse toResponse(Loan loan) {
        LoanResponse response = new LoanResponse();
        response.setId(loan.getId());
        response.setLoanNumber(loan.getLoanNumber());

        if (loan.getLoanType() != null) {
            response.setLoanTypeId(loan.getLoanType().getId());
            response.setLoanTypeName(loan.getLoanType().getName());
        }

        response.setDirection(loan.getDirection());
        response.setCounterpartyType(loan.getCounterpartyType());
        response.setCounterpartyName(loan.getCounterpartyName());
        response.setEmployeeId(loan.getEmployee() != null ? loan.getEmployee().getId() : null);
        response.setCustomerId(loan.getCustomer() != null ? loan.getCustomer().getId() : null);
        response.setSupplierId(loan.getSupplier() != null ? loan.getSupplier().getId() : null);
        response.setPrincipalAmount(loan.getPrincipalAmount());
        response.setCurrency(loan.getCurrency());
        response.setInterestRate(loan.getInterestRate());
        response.setInterestType(loan.getInterestType());
        response.setInterestMethod(loan.getInterestMethod());
        response.setStartDate(loan.getStartDate());
        response.setMaturityDate(loan.getMaturityDate());
        response.setPaymentFrequency(loan.getPaymentFrequency());
        response.setNumberOfInstallments(loan.getNumberOfInstallments());
        response.setRepaymentMethod(loan.getRepaymentMethod());
        response.setOutstandingPrincipal(loan.getOutstandingPrincipal());
        response.setOutstandingInterest(loan.getOutstandingInterest());
        response.setTotalFees(loan.getTotalFees());
        response.setFeeTreatment(loan.getFeeTreatment());
        response.setStatus(loan.getStatus());
        response.setNotes(loan.getNotes());
        response.setSchedule(loan.getSchedule().stream().map(LoanMapper::toLineResponse).toList());
        return response;
    }

    public static LoanListItemResponse toListItemResponse(Loan loan) {
        LoanListItemResponse response = new LoanListItemResponse();
        response.setId(loan.getId());
        response.setLoanNumber(loan.getLoanNumber());
        response.setLoanTypeName(loan.getLoanType() != null ? loan.getLoanType().getName() : null);
        response.setDirection(loan.getDirection());
        response.setCounterpartyName(loan.getCounterpartyName());
        response.setPrincipalAmount(loan.getPrincipalAmount());
        response.setOutstandingPrincipal(loan.getOutstandingPrincipal());
        response.setCurrency(loan.getCurrency());
        response.setStartDate(loan.getStartDate());
        response.setMaturityDate(loan.getMaturityDate());
        response.setStatus(loan.getStatus());
        return response;
    }

    public static LoanRepaymentResponse toRepaymentResponse(LoanRepayment repayment) {
        LoanRepaymentResponse response = new LoanRepaymentResponse();
        response.setId(repayment.getId());
        response.setRepaymentDate(repayment.getRepaymentDate());
        response.setPrincipalAmount(repayment.getPrincipalAmount());
        response.setInterestAmount(repayment.getInterestAmount());
        response.setFeesAmount(repayment.getFeesAmount());
        response.setTotalAmount(repayment.getTotalAmount());
        response.setPaymentMethod(repayment.getPaymentMethod());
        response.setReferenceNumber(repayment.getReferenceNumber());
        response.setMemo(repayment.getMemo());
        return response;
    }

    public static List<LoanRepaymentResponse> toRepaymentResponses(List<LoanRepayment> repayments) {
        return repayments.stream().map(LoanMapper::toRepaymentResponse).toList();
    }
}
