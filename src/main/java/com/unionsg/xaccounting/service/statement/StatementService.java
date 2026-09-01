package com.unionsg.xaccounting.service.statement;

import com.unionsg.xaccounting.dto.statement.StatementLineDto;
import com.unionsg.xaccounting.dto.statement.StatementResponseDto;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.payment.PaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentAllocationEntity;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.payment.PaymentAllocationRepository;
import com.unionsg.xaccounting.repository.payment.SupplierPaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a running-balance statement of activity for a single customer (AR) or
 * supplier (AP) over a date range, combining invoices/bills (debits/charges)
 * with payment allocations (credits/settlements).
 */
@Service
@RequiredArgsConstructor
public class StatementService {

    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillRepository billRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final SupplierPaymentAllocationRepository supplierPaymentAllocationRepository;

    @Transactional(readOnly = true)
    public StatementResponseDto getCustomerStatement(Long customerId, LocalDate fromDate, LocalDate toDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("Customer not found with ID: " + customerId));

        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusMonths(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        List<Invoice> invoices = invoiceRepository.findByCustomerId(customerId).stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .toList();

        List<PaymentAllocationEntity> allocations = paymentAllocationRepository.findByInvoice_CustomerId(customerId);

        List<StatementLineDto> rawLines = new ArrayList<>();
        BigDecimal opening = BigDecimal.ZERO;

        for (Invoice invoice : invoices) {
            if (invoice.getIssueDate().isBefore(from)) {
                opening = opening.add(invoice.getTotalAmount());
            } else if (!invoice.getIssueDate().isAfter(to)) {
                rawLines.add(new StatementLineDto(
                        invoice.getIssueDate(), "INVOICE", invoice.getInvoiceNumber(),
                        "Invoice " + invoice.getInvoiceNumber(),
                        invoice.getTotalAmount(), BigDecimal.ZERO, null
                ));
            }
        }

        for (PaymentAllocationEntity allocation : allocations) {
            LocalDate paymentDate = allocation.getPayment().getPaymentDate();
            if (paymentDate.isBefore(from)) {
                opening = opening.subtract(allocation.getAllocatedAmount());
            } else if (!paymentDate.isAfter(to)) {
                rawLines.add(new StatementLineDto(
                        paymentDate, "PAYMENT", allocation.getPayment().getReceiptNumber(),
                        "Payment applied to " + allocation.getInvoice().getInvoiceNumber(),
                        BigDecimal.ZERO, allocation.getAllocatedAmount(), null
                ));
            }
        }

        List<StatementLineDto> lines = withRunningBalance(rawLines, opening);
        BigDecimal closing = lines.isEmpty() ? opening : lines.get(lines.size() - 1).getBalance();

        return new StatementResponseDto(
                customer.getId(), customer.getCustomerCode(), customer.getDisplayName(),
                customer.getEmail(), customer.getPhone(),
                from, to, opening, lines, closing
        );
    }

    @Transactional(readOnly = true)
    public StatementResponseDto getSupplierStatement(Long supplierId, LocalDate fromDate, LocalDate toDate) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException("Supplier not found with ID: " + supplierId));

        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusMonths(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        List<Bill> bills = billRepository.findBySupplierId(supplierId).stream()
                .filter(bill -> bill.getStatus() != BillStatus.CANCELLED && bill.getStatus() != BillStatus.DRAFT)
                .toList();

        List<SupplierPaymentAllocationEntity> allocations = supplierPaymentAllocationRepository.findByBill_SupplierId(supplierId);

        List<StatementLineDto> rawLines = new ArrayList<>();
        BigDecimal opening = BigDecimal.ZERO;

        for (Bill bill : bills) {
            if (bill.getBillDate().isBefore(from)) {
                opening = opening.add(bill.getTotalAmount());
            } else if (!bill.getBillDate().isAfter(to)) {
                rawLines.add(new StatementLineDto(
                        bill.getBillDate(), "BILL", bill.getBillNumber(),
                        "Bill " + bill.getBillNumber(),
                        bill.getTotalAmount(), BigDecimal.ZERO, null
                ));
            }
        }

        for (SupplierPaymentAllocationEntity allocation : allocations) {
            LocalDate paymentDate = allocation.getSupplierPayment().getPaymentDate();
            if (paymentDate.isBefore(from)) {
                opening = opening.subtract(allocation.getAllocatedAmount());
            } else if (!paymentDate.isAfter(to)) {
                rawLines.add(new StatementLineDto(
                        paymentDate, "PAYMENT", allocation.getSupplierPayment().getPaymentNumber(),
                        "Payment applied to " + allocation.getBill().getBillNumber(),
                        BigDecimal.ZERO, allocation.getAllocatedAmount(), null
                ));
            }
        }

        List<StatementLineDto> lines = withRunningBalance(rawLines, opening);
        BigDecimal closing = lines.isEmpty() ? opening : lines.get(lines.size() - 1).getBalance();

        return new StatementResponseDto(
                supplier.getId(), supplier.getSupplierCode(), supplier.getDisplayName(),
                supplier.getEmail(), supplier.getPhone(),
                from, to, opening, lines, closing
        );
    }

    private List<StatementLineDto> withRunningBalance(List<StatementLineDto> rawLines, BigDecimal opening) {
        rawLines.sort(Comparator.comparing(StatementLineDto::getDate));

        BigDecimal running = opening;
        for (StatementLineDto line : rawLines) {
            running = running.add(line.getDebit()).subtract(line.getCredit());
            line.setBalance(running);
        }
        return rawLines;
    }
}
