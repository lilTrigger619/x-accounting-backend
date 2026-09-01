package com.unionsg.xaccounting.validator;

import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentEntity;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.ChartOfAccountRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class SupplierPaymentValidator {

    private final SupplierRepository supplierRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;

    public Supplier validateSupplierExists(Long supplierId) {
        if (supplierId == null) {
            throw new BadRequestException("Supplier ID is required");
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(
                        "Supplier not found with ID: " + supplierId));
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

    public void validateDraftPayment(SupplierPaymentEntity payment) {
        if (payment == null) {
            throw new BadRequestException("Payment not found");
        }
        if (payment.getStatus() != SupplierPaymentStatus.DRAFT) {
            throw new BusinessException(
                    "Only draft payments can be modified. Current status: " + payment.getStatus());
        }
    }
}
