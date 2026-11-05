package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeRepository;
import com.unionsg.xaccounting.service.branch.BranchAccountingSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * The "subledger journals" setting: while it is on, manual journal lines may name an employee,
 * vendor or customer and the bill or invoice they relate to. These are labels on the ledger
 * only; the bill, invoice and payroll records they name are not changed, and control accounts
 * stay closed to manual journals.
 */
@Service
@RequiredArgsConstructor
public class JournalLineTagService {

    static final String OFF = "Employee, vendor, customer and allocation on journal lines are turned off. "
            + "Turn on subledger journals in Settings > Branch Accounting to use them.";

    private final BranchAccountingSettingsService settingsService;
    private final EmployeeRepository employeeRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final InvoiceRepository invoiceRepository;

    public boolean enabled() {
        return Boolean.TRUE.equals(settingsService.settings().getSubledgerJournalsEnabled());
    }

    public static boolean hasTags(CreateJournalLineRequest r) {
        return r.getEmployeeId() != null || r.getSupplierId() != null || r.getCustomerId() != null
                || r.getBillId() != null || r.getInvoiceId() != null;
    }

    /**
     * Checks the request's tags and sets them on the line. A bill fills in its vendor and an
     * invoice its customer when those are empty, and must belong to them when they're given.
     */
    public void apply(JournalLine line, CreateJournalLineRequest r, boolean enabled) {
        if (!hasTags(r)) {
            return;
        }
        int n = line.getLineNumber() == null ? 0 : line.getLineNumber();
        if (!enabled) {
            throw new BusinessException(OFF);
        }
        if (r.getBillId() != null && r.getInvoiceId() != null) {
            throw new BusinessException("Line " + n + ": allocate it to a bill or an invoice, not both");
        }

        if (r.getEmployeeId() != null) {
            line.setEmployee(employeeRepository.findById(r.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Line " + n + ": employee not found")));
        }
        Supplier supplier = r.getSupplierId() == null ? null : supplierRepository.findById(r.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Line " + n + ": vendor not found"));
        Customer customer = r.getCustomerId() == null ? null : customerRepository.findById(r.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Line " + n + ": customer not found"));

        if (r.getBillId() != null) {
            Bill bill = billRepository.findById(r.getBillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Line " + n + ": bill not found"));
            if (bill.getStatus() == BillStatus.DRAFT || bill.getStatus() == BillStatus.CANCELLED) {
                throw new BusinessException("Line " + n + ": bill " + bill.getBillNumber() + " is "
                        + bill.getStatus().name().toLowerCase() + ", so nothing can be allocated to it");
            }
            if (supplier == null) {
                supplier = bill.getSupplier();
            } else if (bill.getSupplier() != null && !Objects.equals(bill.getSupplier().getId(), supplier.getId())) {
                throw new BusinessException("Line " + n + ": bill " + bill.getBillNumber() + " belongs to "
                        + bill.getSupplier().getDisplayName() + ", not " + supplier.getDisplayName());
            }
            line.setAllocatedBill(bill);
        }
        if (r.getInvoiceId() != null) {
            Invoice invoice = invoiceRepository.findById(r.getInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Line " + n + ": invoice not found"));
            if (invoice.getStatus() == InvoiceStatus.DRAFT || invoice.getStatus() == InvoiceStatus.CANCELLED) {
                throw new BusinessException("Line " + n + ": invoice " + invoice.getInvoiceNumber() + " is "
                        + invoice.getStatus().name().toLowerCase() + ", so nothing can be allocated to it");
            }
            if (customer == null) {
                customer = invoice.getCustomer();
            } else if (invoice.getCustomer() != null && !Objects.equals(invoice.getCustomer().getId(), customer.getId())) {
                throw new BusinessException("Line " + n + ": invoice " + invoice.getInvoiceNumber() + " belongs to "
                        + invoice.getCustomer().getDisplayName() + ", not " + customer.getDisplayName());
            }
            line.setAllocatedInvoice(invoice);
        }
        line.setSupplier(supplier);
        line.setCustomer(customer);
    }
}
