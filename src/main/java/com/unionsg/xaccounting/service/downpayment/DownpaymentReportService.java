package com.unionsg.xaccounting.service.downpayment;

import com.unionsg.xaccounting.MapperLayer.DownpaymentMapper;
import com.unionsg.xaccounting.dto.downpayment.CounterpartyDownpaymentSummary;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentAgingRow;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentAllocationResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentBalanceRow;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentDashboardResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentMovementRow;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentStatementResponse;
import com.unionsg.xaccounting.entity.downpayment.Downpayment;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentAllocation;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentRefund;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.downpayment.DownpaymentAllocationRepository;
import com.unionsg.xaccounting.repository.downpayment.DownpaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Dashboard, statement and reports for downpayments. Every figure is rebuilt from the
 * downpayment, allocation and refund history (including reversals and the dates they
 * happened), so any report can be run as of a past date.
 */
@Service
@RequiredArgsConstructor
public class DownpaymentReportService {

    private final DownpaymentRepository repository;
    private final DownpaymentAllocationRepository allocationRepository;

    /** One dated movement of a downpayment balance. Positive increases what is held. */
    private record Event(LocalDate date, int order, String type, String label, Downpayment dp,
                         String documentNumber, String reference, String description, BigDecimal amount) {}

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    @Transactional(readOnly = true)
    public DownpaymentDashboardResponse dashboard() {
        DownpaymentDashboardResponse r = new DownpaymentDashboardResponse();
        LocalDate today = LocalDate.now();
        for (Downpayment dp : repository.findByDeletedFalse()) {
            DownpaymentDashboardResponse.Side side = dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT ? r.getCustomer() : r.getSupplier();
            if (dp.getStatus() == DownpaymentStatus.DRAFT) {
                side.setDraftCount(side.getDraftCount() + 1);
                continue;
            }
            if (!isPosted(dp) || dp.getStatus() == DownpaymentStatus.REVERSED) continue;
            if (DownpaymentService.USABLE.contains(dp.getStatus())) side.setOpenCount(side.getOpenCount() + 1);
            side.setTotalReceived(side.getTotalReceived().add(dp.getAmount()));
            side.setTotalApplied(side.getTotalApplied().add(dp.getAppliedAmount()));
            side.setTotalRefunded(side.getTotalRefunded().add(dp.getRefundedAmount()));
            side.setAvailableBalance(side.getAvailableBalance().add(dp.getAvailableBalance()));
            if (ChronoUnit.DAYS.between(dp.getPaymentDate(), today) > 90) {
                side.setAgedOver90(side.getAgedOver90().add(dp.getAvailableBalance()));
            }
        }
        r.setRecent(repository.findByDeletedFalse().stream()
                .sorted(Comparator.comparing(Downpayment::getId).reversed())
                .limit(8).map(DownpaymentMapper::toSummary).toList());
        r.setRecentAllocations(allocationRepository.findAllByOrderByAllocationDateAscIdAsc().stream()
                .sorted(Comparator.comparing(DownpaymentAllocation::getId).reversed())
                .limit(8).map(DownpaymentMapper::toAllocationResponse).toList());
        return r;
    }

    @Transactional(readOnly = true)
    public CounterpartyDownpaymentSummary counterpartySummary(DownpaymentType type, Long counterpartyId) {
        requireType(type);
        CounterpartyDownpaymentSummary s = new CounterpartyDownpaymentSummary();
        s.setType(type);
        s.setCounterpartyId(counterpartyId);
        List<Downpayment> list = repository.findByTypeAndDeletedFalse(type).stream()
                .filter(dp -> Objects.equals(counterpartyId, dp.getCounterpartyId()))
                .sorted(Comparator.comparing(Downpayment::getPaymentDate).reversed().thenComparing(Downpayment::getId, Comparator.reverseOrder()))
                .toList();
        for (Downpayment dp : list) {
            if (isPosted(dp) && dp.getStatus() != DownpaymentStatus.REVERSED) {
                s.setCount(s.getCount() + 1);
                s.setTotalAmount(s.getTotalAmount().add(dp.getAmount()));
                s.setAppliedAmount(s.getAppliedAmount().add(dp.getAppliedAmount()));
                s.setRefundedAmount(s.getRefundedAmount().add(dp.getRefundedAmount()));
                s.setAvailableBalance(s.getAvailableBalance().add(dp.getAvailableBalance()));
            }
        }
        s.setDownpayments(list.stream().map(DownpaymentMapper::toSummary).toList());
        return s;
    }

    // =========================================================================
    // REPORTS
    // =========================================================================

    /** Customer downpayment liability (or supplier downpayment asset) per counterparty as of a date. */
    @Transactional(readOnly = true)
    public List<DownpaymentBalanceRow> balances(DownpaymentType type, LocalDate asOf) {
        requireType(type);
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        Map<String, DownpaymentBalanceRow> rows = new LinkedHashMap<>();
        for (Downpayment dp : posted(type)) {
            if (dp.getPaymentDate().isAfter(date)) continue;
            DownpaymentBalanceRow row = rows.computeIfAbsent(key(dp), k -> {
                DownpaymentBalanceRow n = new DownpaymentBalanceRow();
                n.setCounterpartyId(dp.getCounterpartyId());
                n.setCounterpartyName(dp.getCounterpartyName());
                n.setCurrency(dp.getCurrency());
                return n;
            });
            BigDecimal received = BigDecimal.ZERO, applied = BigDecimal.ZERO, refunded = BigDecimal.ZERO;
            for (Event e : events(dp)) {
                if (e.date().isAfter(date)) continue;
                switch (e.type()) {
                    case "RECEIPT", "REVERSAL" -> received = received.add(e.amount());
                    case "APPLICATION", "APPLICATION_REVERSAL" -> applied = applied.subtract(e.amount());
                    default -> refunded = refunded.subtract(e.amount());
                }
            }
            if (received.signum() == 0) continue;
            row.setCount(row.getCount() + 1);
            row.setTotalAmount(row.getTotalAmount().add(received));
            row.setAppliedAmount(row.getAppliedAmount().add(applied));
            row.setRefundedAmount(row.getRefundedAmount().add(refunded));
            row.setAvailableBalance(row.getAvailableBalance().add(received.subtract(applied).subtract(refunded)));
        }
        return rows.values().stream()
                .filter(r -> r.getCount() > 0)
                .sorted(Comparator.comparing(DownpaymentBalanceRow::getAvailableBalance).reversed())
                .toList();
    }

    /** Downpayments that still have money available to apply or refund. */
    @Transactional(readOnly = true)
    public List<DownpaymentResponse> unallocated(DownpaymentType type) {
        return repository.findByDeletedFalse().stream()
                .filter(dp -> type == null || dp.getType() == type)
                .filter(dp -> DownpaymentService.USABLE.contains(dp.getStatus()))
                .filter(dp -> dp.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Downpayment::getPaymentDate))
                .map(DownpaymentMapper::toSummary)
                .toList();
    }

    /** Available balance per counterparty bucketed by how long ago the downpayment was made. */
    @Transactional(readOnly = true)
    public List<DownpaymentAgingRow> aging(DownpaymentType type, LocalDate asOf) {
        requireType(type);
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        Map<String, DownpaymentAgingRow> rows = new LinkedHashMap<>();
        for (Downpayment dp : posted(type)) {
            if (dp.getPaymentDate().isAfter(date)) continue;
            BigDecimal available = events(dp).stream()
                    .filter(e -> !e.date().isAfter(date))
                    .map(Event::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.signum() <= 0) continue;
            DownpaymentAgingRow row = rows.computeIfAbsent(key(dp), k -> {
                DownpaymentAgingRow n = new DownpaymentAgingRow();
                n.setCounterpartyId(dp.getCounterpartyId());
                n.setCounterpartyName(dp.getCounterpartyName());
                n.setCurrency(dp.getCurrency());
                return n;
            });
            long days = ChronoUnit.DAYS.between(dp.getPaymentDate(), date);
            if (days <= 30) row.setDays0To30(row.getDays0To30().add(available));
            else if (days <= 60) row.setDays31To60(row.getDays31To60().add(available));
            else if (days <= 90) row.setDays61To90(row.getDays61To90().add(available));
            else if (days <= 180) row.setDays91To180(row.getDays91To180().add(available));
            else row.setOver180(row.getOver180().add(available));
            row.setTotal(row.getTotal().add(available));
        }
        return rows.values().stream().sorted(Comparator.comparing(DownpaymentAgingRow::getTotal).reversed()).toList();
    }

    /** Every application of downpayments to invoices/bills in a date range. */
    @Transactional(readOnly = true)
    public List<DownpaymentAllocationResponse> allocations(DownpaymentType type, Long counterpartyId,
                                                           LocalDate from, LocalDate to, boolean includeReversed) {
        return allocationRepository.findAllByOrderByAllocationDateAscIdAsc().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getDownpayment().getDeleted()))
                .filter(a -> type == null || a.getDownpayment().getType() == type)
                .filter(a -> counterpartyId == null || counterpartyId.equals(a.getDownpayment().getCounterpartyId()))
                .filter(a -> from == null || !a.getAllocationDate().isBefore(from))
                .filter(a -> to == null || !a.getAllocationDate().isAfter(to))
                .filter(a -> includeReversed || !Boolean.TRUE.equals(a.getReversed()))
                .map(DownpaymentMapper::toAllocationResponse)
                .toList();
    }

    /** Opening balance, receipts, applications, refunds, reversals and closing balance per counterparty. */
    @Transactional(readOnly = true)
    public List<DownpaymentMovementRow> movement(DownpaymentType type, LocalDate from, LocalDate to) {
        requireType(type);
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();
        if (end.isBefore(start)) throw new BusinessException("The end date must be on or after the start date");
        Map<String, DownpaymentMovementRow> rows = new LinkedHashMap<>();
        for (Downpayment dp : posted(type)) {
            DownpaymentMovementRow row = rows.computeIfAbsent(key(dp), k -> {
                DownpaymentMovementRow n = new DownpaymentMovementRow();
                n.setCounterpartyId(dp.getCounterpartyId());
                n.setCounterpartyName(dp.getCounterpartyName());
                n.setCurrency(dp.getCurrency());
                return n;
            });
            for (Event e : events(dp)) {
                if (e.date().isAfter(end)) continue;
                if (e.date().isBefore(start)) {
                    row.setOpeningBalance(row.getOpeningBalance().add(e.amount()));
                    continue;
                }
                switch (e.type()) {
                    case "RECEIPT" -> row.setReceived(row.getReceived().add(e.amount()));
                    case "REVERSAL" -> row.setReversed(row.getReversed().subtract(e.amount()));
                    case "APPLICATION", "APPLICATION_REVERSAL" -> row.setApplied(row.getApplied().subtract(e.amount()));
                    default -> row.setRefunded(row.getRefunded().subtract(e.amount()));
                }
            }
            row.setClosingBalance(row.getOpeningBalance().add(row.getReceived()).subtract(row.getApplied())
                    .subtract(row.getRefunded()).subtract(row.getReversed()));
        }
        return rows.values().stream()
                .filter(r -> Stream.of(r.getOpeningBalance(), r.getReceived(), r.getApplied(), r.getRefunded(),
                        r.getReversed(), r.getClosingBalance()).anyMatch(v -> v.signum() != 0))
                .toList();
    }

    /** Running statement for one customer/supplier, or for a single downpayment. */
    @Transactional(readOnly = true)
    public DownpaymentStatementResponse statement(DownpaymentType type, Long counterpartyId, Long downpaymentId,
                                                  LocalDate from, LocalDate to) {
        List<Downpayment> source;
        DownpaymentStatementResponse s = new DownpaymentStatementResponse();
        if (downpaymentId != null) {
            Downpayment dp = repository.findById(downpaymentId)
                    .filter(d -> !Boolean.TRUE.equals(d.getDeleted()))
                    .orElseThrow(() -> new BusinessException("Downpayment not found with ID: " + downpaymentId));
            source = List.of(dp);
            s.setType(dp.getType());
            s.setCounterpartyId(dp.getCounterpartyId());
            s.setCounterpartyName(dp.getCounterpartyName());
            s.setDownpaymentId(dp.getId());
            s.setDownpaymentNumber(dp.getDownpaymentNumber());
            s.setCurrency(dp.getCurrency());
        } else {
            requireType(type);
            if (counterpartyId == null) throw new BusinessException("Choose a customer or supplier for the statement");
            source = posted(type).stream().filter(dp -> counterpartyId.equals(dp.getCounterpartyId())).toList();
            s.setType(type);
            s.setCounterpartyId(counterpartyId);
            s.setCounterpartyName(source.isEmpty() ? null : source.get(0).getCounterpartyName());
            s.setCurrency(source.stream().map(Downpayment::getCurrency).distinct().count() == 1 ? source.get(0).getCurrency() : null);
        }
        s.setFromDate(from);
        s.setToDate(to);

        List<Event> all = source.stream().filter(this::isPosted).flatMap(dp -> events(dp).stream())
                .sorted(Comparator.comparing(Event::date).thenComparing(Event::order))
                .toList();
        BigDecimal running = BigDecimal.ZERO;
        for (Event e : all) {
            if (to != null && e.date().isAfter(to)) continue;
            if (from != null && e.date().isBefore(from)) {
                running = running.add(e.amount());
                continue;
            }
            if (s.getLines().isEmpty()) s.setOpeningBalance(running);
            running = running.add(e.amount());
            DownpaymentStatementResponse.Line line = new DownpaymentStatementResponse.Line();
            line.setDate(e.date());
            line.setEntryType(e.type());
            line.setEntryLabel(e.label());
            line.setDownpaymentId(e.dp().getId());
            line.setDownpaymentNumber(e.dp().getDownpaymentNumber());
            line.setDocumentNumber(e.documentNumber());
            line.setReference(e.reference());
            line.setDescription(e.description());
            if (e.amount().signum() >= 0) {
                line.setIncrease(e.amount());
                s.setTotalIncrease(s.getTotalIncrease().add(e.amount()));
            } else {
                line.setDecrease(e.amount().negate());
                s.setTotalDecrease(s.getTotalDecrease().add(e.amount().negate()));
            }
            line.setBalance(running);
            s.getLines().add(line);
        }
        if (s.getLines().isEmpty()) s.setOpeningBalance(running);
        s.setClosingBalance(running);
        return s;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private List<Event> events(Downpayment dp) {
        List<Event> out = new ArrayList<>();
        if (!isPosted(dp)) return out;
        boolean customer = dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT;
        out.add(new Event(dp.getPaymentDate(), 0, "RECEIPT", customer ? "Downpayment received" : "Downpayment paid",
                dp, dp.getDownpaymentNumber(), dp.getReference(), dp.getDescription(), dp.getAmount()));
        for (DownpaymentAllocation a : dp.getAllocations()) {
            out.add(new Event(a.getAllocationDate(), 1, "APPLICATION", "Applied to " + a.getDocumentNumber(),
                    dp, a.getDocumentNumber(), null, a.getNotes(), a.getAmount().negate()));
            if (Boolean.TRUE.equals(a.getReversed()) && a.getReversedAt() != null) {
                out.add(new Event(a.getReversedAt().toLocalDate(), 2, "APPLICATION_REVERSAL",
                        "Application to " + a.getDocumentNumber() + " reversed",
                        dp, a.getDocumentNumber(), null, a.getReversalReason(), a.getAmount()));
            }
        }
        for (DownpaymentRefund f : dp.getRefunds()) {
            out.add(new Event(f.getRefundDate(), 3, "REFUND", customer ? "Refunded to customer" : "Refund received from supplier",
                    dp, f.getRefundNumber(), f.getReference(), f.getReason(), f.getAmount().negate()));
            if (Boolean.TRUE.equals(f.getReversed()) && f.getReversedAt() != null) {
                out.add(new Event(f.getReversedAt().toLocalDate(), 4, "REFUND_REVERSAL", "Refund " + f.getRefundNumber() + " reversed",
                        dp, f.getRefundNumber(), f.getReference(), f.getReversalReason(), f.getAmount()));
            }
        }
        if (dp.getStatus() == DownpaymentStatus.REVERSED && dp.getReversedAt() != null) {
            out.add(new Event(dp.getReversedAt().toLocalDate(), 5, "REVERSAL", "Downpayment reversed",
                    dp, dp.getDownpaymentNumber(), dp.getReference(), dp.getReversalReason(), dp.getAmount().negate()));
        }
        return out;
    }

    private List<Downpayment> posted(DownpaymentType type) {
        return repository.findByTypeAndDeletedFalse(type).stream().filter(this::isPosted).toList();
    }

    private boolean isPosted(Downpayment dp) {
        return dp.getJournal() != null && dp.getStatus() != DownpaymentStatus.DRAFT && dp.getStatus() != DownpaymentStatus.CANCELLED;
    }

    private static String key(Downpayment dp) {
        return dp.getCounterpartyId() + "|" + dp.getCurrency();
    }

    private static void requireType(DownpaymentType type) {
        if (type == null) throw new BusinessException("Choose customer or supplier downpayments");
    }
}
