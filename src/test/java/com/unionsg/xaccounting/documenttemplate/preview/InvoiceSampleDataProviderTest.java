package com.unionsg.xaccounting.documenttemplate.preview;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.invoice.InvoiceItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceSampleDataProviderTest {

    private final InvoiceSampleDataProvider provider = new InvoiceSampleDataProvider();

    @Test
    void supportsInvoiceDocumentType() {
        assertThat(provider.getDocumentType()).isEqualTo(DocumentType.INVOICE);
    }

    @Test
    void buildsRealisticInvoiceSampleData() {
        Object data = provider.buildSampleData();

        assertThat(data).isInstanceOf(Invoice.class);

        Invoice invoice = (Invoice) data;
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-2026-0001");
        assertThat(invoice.getReference()).isEqualTo("PO-2026-0192");
        assertThat(invoice.getCustomer()).isNotNull();
        assertThat(invoice.getCustomer().getDisplayName()).isEqualTo("Kwame Mensah Enterprises");
        assertThat(invoice.getCustomer().getEmail()).isEqualTo("kwame@example.com");
        assertThat(invoice.getCustomer().getPhone()).isEqualTo("+233 24 555 1234");
        assertThat(invoice.getBillingInfo()).isNotNull();
        assertThat(invoice.getBillingInfo().getAddressLine1()).isEqualTo("45 Oxford Street");
        assertThat(invoice.getBillingInfo().getCountry()).isEqualTo("Ghana");
    }

    @Test
    void buildsRealisticLineItemsAndTotals() {
        Invoice invoice = (Invoice) provider.buildSampleData();

        assertThat(invoice.getItems()).hasSize(3);

        InvoiceItem first = invoice.getItems().get(0);
        assertThat(first.getDescription()).isEqualTo("Website Development");
        assertThat(first.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(2500.00));

        assertThat(invoice.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(3550.00));
        assertThat(invoice.getTotalTax()).isEqualByComparingTo(BigDecimal.valueOf(284.00));
        assertThat(invoice.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(3734.00));
        assertThat(invoice.getAmountPaid()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
        assertThat(invoice.getTotalDue()).isEqualByComparingTo(BigDecimal.valueOf(2734.00));
    }

    @Test
    void sampleDataIncludesTermsNotesAndPaymentTerms() {
        Invoice invoice = (Invoice) provider.buildSampleData();

        assertThat(invoice.getTerms()).isNotBlank();
        assertThat(invoice.getNotes()).isNotBlank();
        assertThat(invoice.getPaymentTerms()).isNotNull();
        assertThat(invoice.getPaymentTerms().getPaymentTermType()).isNotNull();
    }
}
