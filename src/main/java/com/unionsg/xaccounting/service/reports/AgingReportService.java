package com.unionsg.xaccounting.service.reports;

import com.unionsg.xaccounting.dto.reports.AgingReportResponseDto;
import com.unionsg.xaccounting.dto.reports.AgingRowDto;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes Accounts Receivable and Accounts Payable aging (current / 1-30 / 31-60 / 61-90 / 90+)
 * from the outstanding balances already tracked on Invoices and Bills.
 */
@Service
@RequiredArgsConstructor
public class AgingReportService {

    private final InvoiceRepository invoiceRepository;
    private final BillRepository billRepository;

    @Transactional(readOnly = true)
    public AgingReportResponseDto getArAging(LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate != null ? asOfDate : LocalDate.now();

        List<Invoice> openInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED
                        && inv.getStatus() != InvoiceStatus.PAID
                        && inv.getBalance() != null
                        && inv.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        Map<Long, AgingRowDto> byCustomer = new LinkedHashMap<>();

        for (Invoice invoice : openInvoices) {
            Long customerId = invoice.getCustomer().getId();
            AgingRowDto row = byCustomer.computeIfAbsent(customerId, id -> newRow(
                    id,
                    invoice.getCustomer().getCustomerCode(),
                    invoice.getCustomer().getDisplayName()
            ));

            applyBucket(row, invoice.getDueDate(), invoice.getBalance(), effectiveDate);
        }

        return buildResponse(effectiveDate, byCustomer);
    }

    @Transactional(readOnly = true)
    public AgingReportResponseDto getApAging(LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate != null ? asOfDate : LocalDate.now();

        List<Bill> openBills = billRepository.findAll().stream()
                .filter(bill -> bill.getStatus() != BillStatus.CANCELLED
                        && bill.getStatus() != BillStatus.DRAFT
                        && bill.getStatus() != BillStatus.PAID
                        && bill.getBalance() != null
                        && bill.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        Map<Long, AgingRowDto> bySupplier = new LinkedHashMap<>();

        for (Bill bill : openBills) {
            Long supplierId = bill.getSupplier().getId();
            AgingRowDto row = bySupplier.computeIfAbsent(supplierId, id -> newRow(
                    id,
                    bill.getSupplier().getSupplierCode(),
                    bill.getSupplier().getDisplayName()
            ));

            applyBucket(row, bill.getDueDate(), bill.getBalance(), effectiveDate);
        }

        return buildResponse(effectiveDate, bySupplier);
    }

    private AgingRowDto newRow(Long partyId, String partyCode, String partyName) {
        return new AgingRowDto(
                partyId, partyCode, partyName,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }

    private void applyBucket(AgingRowDto row, LocalDate dueDate, BigDecimal outstanding, LocalDate asOfDate) {
        long daysPastDue = ChronoUnit.DAYS.between(dueDate, asOfDate);

        if (daysPastDue <= 0) {
            row.setCurrent(row.getCurrent().add(outstanding));
        } else if (daysPastDue <= 30) {
            row.setDays1To30(row.getDays1To30().add(outstanding));
        } else if (daysPastDue <= 60) {
            row.setDays31To60(row.getDays31To60().add(outstanding));
        } else if (daysPastDue <= 90) {
            row.setDays61To90(row.getDays61To90().add(outstanding));
        } else {
            row.setOver90(row.getOver90().add(outstanding));
        }

        row.setTotal(row.getTotal().add(outstanding));
    }

    private AgingReportResponseDto buildResponse(LocalDate asOfDate, Map<Long, AgingRowDto> byParty) {
        List<AgingRowDto> rows = byParty.values().stream()
                .sorted(Comparator.comparing(AgingRowDto::getTotal).reversed())
                .collect(Collectors.toList());

        AgingRowDto totals = newRow(null, null, "Total");
        for (AgingRowDto row : rows) {
            totals.setCurrent(totals.getCurrent().add(row.getCurrent()));
            totals.setDays1To30(totals.getDays1To30().add(row.getDays1To30()));
            totals.setDays31To60(totals.getDays31To60().add(row.getDays31To60()));
            totals.setDays61To90(totals.getDays61To90().add(row.getDays61To90()));
            totals.setOver90(totals.getOver90().add(row.getOver90()));
            totals.setTotal(totals.getTotal().add(row.getTotal()));
        }

        return new AgingReportResponseDto(asOfDate, rows, totals);
    }
}
