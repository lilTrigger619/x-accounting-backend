package com.unionsg.xaccounting.service.prepayment;

import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentRequest;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentType;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentCounterpartyType;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentFrequency;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeRepository;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentRepository;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentTypeRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepaymentServiceValidationTest {

    @Mock private PrepaymentRepository repository;
    @Mock private PrepaymentTypeRepository typeRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private DocumentNumberService documentNumberService;
    @Mock private PrepaymentJournalService journalService;
    @Mock private ConfigValueValidator configValues;

    @InjectMocks
    private PrepaymentService service;

    private static CreatePrepaymentRequest request() {
        CreatePrepaymentRequest r = new CreatePrepaymentRequest();
        r.setPrepaymentTypeId(1L);
        r.setCounterpartyType(PrepaymentCounterpartyType.SUPPLIER);
        r.setSupplierId(5L);
        r.setTotalAmount(new BigDecimal("1200"));
        r.setCurrency("USD");
        r.setPaymentDate(LocalDate.of(2026, 10, 1));
        r.setRecognitionStartDate(LocalDate.of(2026, 10, 1));
        r.setNumberOfPeriods(12);
        r.setRecognitionFrequency(PrepaymentFrequency.MONTHLY);
        return r;
    }

    private static PrepaymentType activeType() {
        PrepaymentType type = new PrepaymentType();
        type.setName("Insurance");
        type.setActive(true);
        type.setDeleted(false);
        return type;
    }

    private void assertRejected(CreatePrepaymentRequest r, String message) {
        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(BusinessException.class)
                .hasMessage(message);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMissingType() {
        CreatePrepaymentRequest r = request();
        r.setPrepaymentTypeId(null);
        assertRejected(r, "Choose a prepayment type");
    }

    @Test
    void rejectsMissingCounterpartyType() {
        CreatePrepaymentRequest r = request();
        r.setCounterpartyType(null);
        assertRejected(r, "Choose a counterparty type");
    }

    @Test
    void rejectsMissingRecognitionFrequency() {
        CreatePrepaymentRequest r = request();
        r.setRecognitionFrequency(null);
        assertRejected(r, "Choose a recognition frequency");
    }

    @Test
    void rejectsMissingRecognitionStartDate() {
        CreatePrepaymentRequest r = request();
        r.setRecognitionStartDate(null);
        assertRejected(r, "Recognition start date is required");
    }

    @Test
    void rejectsSupplierCounterpartyWithoutSupplier() {
        when(typeRepository.findById(1L)).thenReturn(Optional.of(activeType()));
        CreatePrepaymentRequest r = request();
        r.setSupplierId(null);
        assertRejected(r, "Choose a supplier");
    }

    @Test
    void rejectsCurrencyOutsideConfiguration() {
        when(typeRepository.findById(1L)).thenReturn(Optional.of(activeType()));
        when(supplierRepository.findById(5L)).thenReturn(Optional.of(new Supplier()));
        when(configValues.require("currencies", "GHC", null, "Currency"))
                .thenThrow(new BusinessException("\"GHC\" is not a valid currency. Pick one from the list or add it first."));
        CreatePrepaymentRequest r = request();
        r.setCurrency("GHC");
        assertRejected(r, "\"GHC\" is not a valid currency. Pick one from the list or add it first.");
    }
}
