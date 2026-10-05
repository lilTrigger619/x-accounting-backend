package com.unionsg.xaccounting.service.invoice;

import com.unionsg.xaccounting.dto.invoice.CreateInvoiceRequest;
import com.unionsg.xaccounting.dto.invoice.InvoiceItemRequest;
import com.unionsg.xaccounting.enums.DiscountType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.CustomerPaymentTermsRepo;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.product.ProductRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.service.customer.CustomerActivityLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceValidationTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerPaymentTermsRepo paymentTermsRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InvoiceCalculationService calculationService;
    @Mock private DocumentNumberService generator;
    @Mock private FileService fileService;
    @Mock private CustomerActivityLogService customerActivityLogService;
    @Mock private InvoiceJournalService invoiceJournalService;

    @InjectMocks
    private InvoiceService service;

    private static CreateInvoiceRequest request() {
        InvoiceItemRequest item = new InvoiceItemRequest();
        item.setDescription("Consulting");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal("100"));
        item.setTaxRate(BigDecimal.ZERO);

        CreateInvoiceRequest r = new CreateInvoiceRequest();
        r.setCustomerId(1L);
        r.setIssueDate(LocalDate.of(2026, 10, 1));
        r.setDueDate(LocalDate.of(2026, 10, 31));
        r.setDiscountType(DiscountType.NONE);
        r.setItems(List.of(item));
        return r;
    }

    private void assertRejected(CreateInvoiceRequest r, String message) {
        assertThatThrownBy(() -> service.createInvoice(null, r))
                .isInstanceOf(BusinessException.class)
                .hasMessage(message);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void rejectsMissingCustomer() {
        CreateInvoiceRequest r = request();
        r.setCustomerId(null);
        assertRejected(r, "Choose a customer for this invoice");
    }

    @Test
    void rejectsUnknownCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertRejected(request(), "Customer not found");
    }

    @Test
    void rejectsDueDateBeforeIssueDate() {
        CreateInvoiceRequest r = request();
        r.setDueDate(LocalDate.of(2026, 9, 30));
        assertRejected(r, "Due date can't be before the issue date");
    }

    @Test
    void rejectsInvoiceWithoutItems() {
        CreateInvoiceRequest r = request();
        r.setItems(List.of());
        assertRejected(r, "Add at least one line item");
    }

    @Test
    void rejectsPercentageDiscountOverOneHundred() {
        CreateInvoiceRequest r = request();
        r.setDiscountType(DiscountType.PERCENTAGE);
        r.setDiscountValue(new BigDecimal("150"));
        assertRejected(r, "A percentage discount can't be more than 100%");
    }

    @Test
    void rejectsNegativeFixedDiscount() {
        CreateInvoiceRequest r = request();
        r.setDiscountType(DiscountType.FIXED);
        r.setDiscountValue(new BigDecimal("-5"));
        assertRejected(r, "Enter a discount value of zero or more");
    }
}
