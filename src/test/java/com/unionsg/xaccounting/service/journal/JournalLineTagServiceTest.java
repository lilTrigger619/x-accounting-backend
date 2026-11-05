package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeRepository;
import com.unionsg.xaccounting.service.branch.BranchAccountingSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalLineTagServiceTest {

    @Mock private BranchAccountingSettingsService settingsService;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BillRepository billRepository;
    @Mock private InvoiceRepository invoiceRepository;

    @InjectMocks
    private JournalLineTagService service;

    private static Supplier supplier(long id, String name) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setDisplayName(name);
        return s;
    }

    private static Bill bill(long id, Supplier supplier, BillStatus status) {
        Bill b = new Bill();
        b.setId(id);
        b.setBillNumber("BL-" + id);
        b.setSupplier(supplier);
        b.setStatus(status);
        return b;
    }

    private static JournalLine line() {
        JournalLine l = new JournalLine();
        l.setLineNumber(2);
        return l;
    }

    @Test
    void untaggedLinesPassEvenWhenTheSettingIsOff() {
        assertThatCode(() -> service.apply(line(), new CreateJournalLineRequest(), false)).doesNotThrowAnyException();
    }

    @Test
    void tagsAreRejectedWhileTheSettingIsOff() {
        CreateJournalLineRequest r = new CreateJournalLineRequest();
        r.setSupplierId(1L);
        assertThatThrownBy(() -> service.apply(line(), r, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Settings > Branch Accounting");
    }

    @Test
    void aBillFillsInItsVendor() {
        Supplier acme = supplier(1L, "Acme");
        when(billRepository.findById(5L)).thenReturn(Optional.of(bill(5L, acme, BillStatus.OPEN)));
        CreateJournalLineRequest r = new CreateJournalLineRequest();
        r.setBillId(5L);
        JournalLine l = line();

        service.apply(l, r, true);

        assertThat(l.getSupplier()).isSameAs(acme);
        assertThat(l.getAllocatedBill().getBillNumber()).isEqualTo("BL-5");
    }

    @Test
    void aBillMustBelongToTheChosenVendor() {
        when(supplierRepository.findById(2L)).thenReturn(Optional.of(supplier(2L, "Globex")));
        when(billRepository.findById(5L)).thenReturn(Optional.of(bill(5L, supplier(1L, "Acme"), BillStatus.OPEN)));
        CreateJournalLineRequest r = new CreateJournalLineRequest();
        r.setSupplierId(2L);
        r.setBillId(5L);

        assertThatThrownBy(() -> service.apply(line(), r, true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Line 2: bill BL-5 belongs to Acme, not Globex");
    }

    @Test
    void draftBillsAndDoubleAllocationsAreRejected() {
        when(billRepository.findById(5L)).thenReturn(Optional.of(bill(5L, supplier(1L, "Acme"), BillStatus.DRAFT)));
        CreateJournalLineRequest draft = new CreateJournalLineRequest();
        draft.setBillId(5L);
        assertThatThrownBy(() -> service.apply(line(), draft, true)).hasMessageContaining("is draft");

        CreateJournalLineRequest both = new CreateJournalLineRequest();
        both.setBillId(5L);
        both.setInvoiceId(9L);
        assertThatThrownBy(() -> service.apply(line(), both, true)).hasMessageContaining("not both");
    }
}
