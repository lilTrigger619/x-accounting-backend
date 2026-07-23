package com.unionsg.xaccounting.validator;

import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.payment.PaymentEntity;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.PaymentStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.ChartOfAccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.utils.PaymentConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentValidator {

    private final CustomerRepository customerRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final InvoiceRepository invoiceRepository;

    public Customer validateCustomerExists(Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("Customer ID is required");
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(
                        "Customer not found with ID: " + customerId));
    }

    public void validateInvoicesBelongToCustomer(List<Long> invoiceIds, Long customerId) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return;
        }
        for (Long invoiceId : invoiceIds) {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new BusinessException(
                            "Invoice not found with ID: " + invoiceId));
            if (!invoice.getCustomer().getId().equals(customerId)) {
                throw new BusinessException(
                        "Invoice " + invoice.getInvoiceNumber()
                                + " does not belong to the specified customer");
            }
        }
    }

    public ChartOfAccount validateBankAccount(Long bankAccountId) {
        if (bankAccountId == null) {
            throw new BadRequestException("Bank account ID is required");
        }
        return chartOfAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new BusinessException(
                        "Bank account not found with ID: " + bankAccountId));
    }

    public void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(fieldName + " must be positive");
        }
    }

    public void validateCurrency(Currency currency) {
        if (currency == null) {
            throw new BadRequestException("Currency is required");
        }
    }

    public void validateExchangeRate(BigDecimal exchangeRate) {
        if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Exchange rate cannot be negative");
        }
    }

    public void validatePaymentStatus(PaymentStatus currentStatus, PaymentStatus... allowedStatuses) {
        if (currentStatus == null) {
            throw new BadRequestException("Payment status is required");
        }
        if (allowedStatuses == null || allowedStatuses.length == 0) {
            return;
        }
        for (PaymentStatus allowed : allowedStatuses) {
            if (currentStatus == allowed) {
                return;
            }
        }
        throw new BusinessException(
                "Invalid payment status transition. Current status: " + currentStatus);
    }

    public void validateDraftPayment(PaymentEntity payment) {
        if (payment == null) {
            throw new BadRequestException("Payment not found");
        }
        if (payment.getStatus() != PaymentStatus.DRAFT) {
            throw new BusinessException(
                    "Only draft payments can be modified. Current status: " + payment.getStatus());
        }
    }

    public void validateReceiptNumber(String receiptNumber) {
        if (receiptNumber == null || receiptNumber.isBlank()) {
            throw new BadRequestException("Receipt number is required");
        }
        if (receiptNumber.length() > PaymentConstants.MAX_REFERENCE_LENGTH) {
            throw new BadRequestException(
                    "Receipt number must not exceed " + PaymentConstants.MAX_REFERENCE_LENGTH + " characters");
        }
    }

    public void validateRefund(BigDecimal unallocatedAmount, BigDecimal refundAmount) {
        if (unallocatedAmount == null) {
            throw new BadRequestException("Unallocated amount is required to process refund");
        }
        if (refundAmount == null) {
            throw new BadRequestException("Refund amount is required");
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be positive");
        }
        if (refundAmount.compareTo(unallocatedAmount) > 0) {
            throw new BusinessException(
                    "Refund amount " + refundAmount
                            + " cannot exceed unallocated amount " + unallocatedAmount);
        }
    }
}
