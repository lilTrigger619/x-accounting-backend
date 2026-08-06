package com.unionsg.xaccounting.documenttemplate.preview;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.embeddables.InvoiceBillingInfo;
import com.unionsg.xaccounting.entity.customer.Address;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.invoice.InvoiceItem;
import com.unionsg.xaccounting.enums.AddressType;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.CustomerStatus;
import com.unionsg.xaccounting.enums.CustomerType;
import com.unionsg.xaccounting.enums.DiscountType;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.enums.PaymentTermType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Builds a realistic, in-memory sample {@link Invoice} for the Document Template
 * Designer sample preview.
 *
 * <p>This provider returns the SAME {@link Invoice} entity model used by the real
 * invoice rendering pipeline, ensuring the sample preview renders through the exact
 * same Thymeleaf template structure.</p>
 *
 * <p>No database records are created, no invoice number sequences are consumed,
 * and no account balances are mutated. The returned object exists only for the
 * duration of the HTTP request.</p>
 */
@Component
public class InvoiceSampleDataProvider implements DocumentPreviewSampleDataProvider {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.INVOICE;
    }

    @Override
    public Object buildSampleData() {
        return buildInvoice();
    }

    private Invoice buildInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-2026-0001");
        invoice.setReference("PO-2026-0192");
        invoice.setIssueDate(LocalDate.of(2026, 8, 5));
        invoice.setDueDate(LocalDate.of(2026, 9, 4));
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        invoice.setCurrency("GHS");
        invoice.setNotes("Thank you for your business. Please direct all payment inquiries to billing@acme.example.");
        invoice.setTerms("Payment is due within 30 days of the invoice date. Late payments may incur a 2% monthly "
                + "service charge. Please reference the invoice number on all payments.");

        invoice.setCustomer(buildCustomer());
        invoice.setBillingInfo(buildBillingInfo());
        invoice.setPaymentTerms(buildPaymentTerms());

        // Build line items and compute totals
        invoice.setItems(buildItems(invoice));

        invoice.setSubtotal(BigDecimal.valueOf(3550.00));
        invoice.setTotalTax(BigDecimal.valueOf(284.00));
        invoice.setDiscountType(DiscountType.FIXED);
        invoice.setDiscountValue(BigDecimal.valueOf(100.00));
        invoice.setDiscountAmount(BigDecimal.valueOf(100.00));
        invoice.setTotalAmount(BigDecimal.valueOf(3734.00));
        invoice.setAmountPaid(BigDecimal.valueOf(1000.00));
        invoice.setBalance(BigDecimal.valueOf(2734.00));
        invoice.setTotalDue(BigDecimal.valueOf(2734.00));

        return invoice;
    }

    private Customer buildCustomer() {
        Address billingAddress = Address.builder()
                .addressLine("45 Oxford Street")
                .city("Osu")
                .state("Greater Accra")
                .zipCode("GA-123")
                .country("Ghana")
                .addressType(AddressType.BILLING)
                .build();

        Address shippingAddress = Address.builder()
                .addressLine("45 Oxford Street")
                .city("Osu")
                .state("Greater Accra")
                .zipCode("GA-123")
                .country("Ghana")
                .addressType(AddressType.SHIPPING)
                .build();

        return Customer.builder()
                .id(1L)
                .customerCode("CUS-0001")
                .customerType(CustomerType.BUSN)
                .companyName("Kwame Mensah Enterprises")
                .displayName("Kwame Mensah Enterprises")
                .status(CustomerStatus.ACTIVE)
                .email("kwame@example.com")
                .phone("+233 24 555 1234")
                .billingAddress(billingAddress)
                .shippingAddress(shippingAddress)
                .build();
    }

    private InvoiceBillingInfo buildBillingInfo() {
        InvoiceBillingInfo billingInfo = new InvoiceBillingInfo();
        billingInfo.setBillingName("Kwame Mensah Enterprises");
        billingInfo.setBillingEmail("kwame@example.com");
        billingInfo.setBillingPhone("+233 24 555 1234");
        billingInfo.setAddressLine1("45 Oxford Street");
        billingInfo.setAddressLine2("Osu");
        billingInfo.setCity("Accra");
        billingInfo.setState("Greater Accra");
        billingInfo.setPostalCode("GA-123");
        billingInfo.setCountry("Ghana");
        return billingInfo;
    }

    private PaymentTerms buildPaymentTerms() {
        PaymentTerms paymentTerms = new PaymentTerms();
        paymentTerms.setPaymentTermType(PaymentTermType.NET30);
        paymentTerms.setCreditLimit(BigDecimal.valueOf(50000.00));
        paymentTerms.setCurrency(Currency.GHC);
        return paymentTerms;
    }

    private List<InvoiceItem> buildItems(Invoice invoice) {
        return List.of(
                buildItem(invoice, "Website Development", 1, 2500.00, 0, 2500.00, 0, 2500.00),
                buildItem(invoice, "Consulting Services", 5, 150.00, 8, 750.00, 60.00, 810.00),
                buildItem(invoice, "Hosting & Maintenance (Annual)", 1, 300.00, 8, 300.00, 24.00, 324.00)
        );
    }

    private InvoiceItem buildItem(Invoice invoice,
                                  String description,
                                  double quantity,
                                  double unitPrice,
                                  double taxRate,
                                  double lineSubtotal,
                                  double lineTax,
                                  double lineTotal) {
        InvoiceItem item = new InvoiceItem();
        item.setDescription(description);
        item.setQuantity(BigDecimal.valueOf(quantity));
        item.setUnitPrice(BigDecimal.valueOf(unitPrice));
        item.setTaxRate(BigDecimal.valueOf(taxRate));
        item.setLineSubtotal(BigDecimal.valueOf(lineSubtotal));
        item.setLineTax(BigDecimal.valueOf(lineTax));
        item.setLineTotal(BigDecimal.valueOf(lineTotal));
        item.setInvoice(invoice);
        return item;
    }
}
