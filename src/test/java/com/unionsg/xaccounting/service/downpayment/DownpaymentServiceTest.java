package com.unionsg.xaccounting.service.downpayment;

import com.unionsg.xaccounting.dto.downpayment.AllocateDownpaymentRequest;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentResponse;
import com.unionsg.xaccounting.dto.downpayment.RefundDownpaymentRequest;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.downpayment.Downpayment;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentAllocation;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentRefund;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import com.unionsg.xaccounting.enums.settlement.SettlementSourceType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.downpayment.DownpaymentAllocationRepository;
import com.unionsg.xaccounting.repository.downpayment.DownpaymentRefundRepository;
import com.unionsg.xaccounting.repository.downpayment.DownpaymentRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.settlement.DocumentSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DownpaymentServiceTest {

    @Mock private DownpaymentRepository repository;
    @Mock private DownpaymentAllocationRepository allocationRepository;
    @Mock private DownpaymentRefundRepository refundRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private BillRepository billRepository;
    @Mock private DocumentNumberService documentNumberService;
    @Mock private DownpaymentJournalService journalService;
    @Mock private DocumentSettlementService settlementService;
    @Mock private PeriodLockGuard periodLockGuard;

    @InjectMocks private DownpaymentService service;

    private Downpayment dp;
    private final List<DownpaymentAllocation> savedAllocations = new ArrayList<>();
    private final List<DownpaymentRefund> savedRefunds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setId(7L);
        customer.setDisplayName("Acme Ltd");

        BankAccount bank = new BankAccount();
        bank.setId(1L);
        bank.setAccountName("Main");
        bank.setCurrency("USD");

        dp = new Downpayment();
        dp.setId(10L);
        dp.setDownpaymentNumber("DP-0001");
        dp.setType(DownpaymentType.CUSTOMER_DOWNPAYMENT);
        dp.setCustomer(customer);
        dp.setCounterpartyName("Acme Ltd");
        dp.setAmount(new BigDecimal("3000.00"));
        dp.setCurrency("USD");
        dp.setPaymentDate(LocalDate.of(2026, 9, 1));
        dp.setBankAccount(bank);
        dp.setStatus(DownpaymentStatus.OPEN);
        dp.setAvailableBalance(new BigDecimal("3000.00"));
        dp.setJournal(new JournalEntry());

        when(repository.findById(10L)).thenReturn(Optional.of(dp));
        when(repository.save(any(Downpayment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.getReferenceById(anyLong())).thenAnswer(inv -> {
            Invoice i = new Invoice();
            i.setId(inv.getArgument(0));
            i.setInvoiceNumber("INV-" + inv.getArgument(0));
            return i;
        });
        when(allocationRepository.save(any(DownpaymentAllocation.class))).thenAnswer(inv -> {
            DownpaymentAllocation a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId((long) savedAllocations.size() + 100);
                savedAllocations.add(a);
            }
            return a;
        });
        when(allocationRepository.findByDownpaymentIdOrderByAllocationDateAscIdAsc(10L)).thenReturn(savedAllocations);
        when(refundRepository.save(any(DownpaymentRefund.class))).thenAnswer(inv -> {
            DownpaymentRefund r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId((long) savedRefunds.size() + 200);
                savedRefunds.add(r);
            }
            return r;
        });
        when(refundRepository.findByDownpaymentIdOrderByRefundDateAscIdAsc(10L)).thenReturn(savedRefunds);
    }

    private static AllocateDownpaymentRequest allocate(Object... idAmountPairs) {
        AllocateDownpaymentRequest request = new AllocateDownpaymentRequest();
        request.setAllocationDate(LocalDate.of(2026, 9, 15));
        for (int i = 0; i < idAmountPairs.length; i += 2) {
            AllocateDownpaymentRequest.Line line = new AllocateDownpaymentRequest.Line();
            line.setDocumentId(((Number) idAmountPairs[i]).longValue());
            line.setAmount(new BigDecimal((String) idAmountPairs[i + 1]));
            request.getLines().add(line);
        }
        return request;
    }

    @Test
    void partialApplicationLeavesTheRestAvailable() {
        DownpaymentResponse response = service.allocate(10L, allocate(1, "1000.00"));

        assertThat(response.getAppliedAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("2000.00");
        assertThat(response.getStatus()).isEqualTo(DownpaymentStatus.PARTIALLY_APPLIED);
        verify(settlementService).settleInvoice(eq(1L), eq(7L), eq("USD"), any(), any(),
                eq(SettlementSourceType.DOWNPAYMENT), eq(10L), eq(100L), eq("DP-0001"));
        verify(journalService).postAllocation(eq(dp), any(DownpaymentAllocation.class));
    }

    @Test
    void multipleInvoicesCanUseTheWholeDownpayment() {
        DownpaymentResponse response = service.allocate(10L, allocate(1, "1000.00", 2, "2000.00"));

        assertThat(response.getAvailableBalance()).isEqualByComparingTo("0");
        assertThat(response.getStatus()).isEqualTo(DownpaymentStatus.FULLY_APPLIED);
        assertThat(savedAllocations).hasSize(2);
    }

    @Test
    void refusesToApplyMoreThanIsAvailable() {
        assertThatThrownBy(() -> service.allocate(10L, allocate(1, "2000.00", 2, "1500.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds the available balance");
        verify(journalService, never()).postAllocation(any(), any());
    }

    @Test
    void refusesTheSameInvoiceTwiceInOneAllocation() {
        assertThatThrownBy(() -> service.allocate(10L, allocate(1, "100.00", 1, "100.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("more than once");
    }

    @Test
    void refusesAnInvoiceTheSharedSettlementRejects() {
        doThrow(new BusinessException("Invoice INV-1 belongs to a different customer"))
                .when(settlementService).validateInvoiceSettlement(eq(1L), eq(7L), eq("USD"), any());

        assertThatThrownBy(() -> service.allocate(10L, allocate(1, "100.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("different customer");
        verify(allocationRepository, never()).save(any());
    }

    @Test
    void refusesALockedPeriod() {
        doThrow(new BadRequestException("Accounting period is locked"))
                .when(periodLockGuard).assertPostable(LocalDate.of(2026, 9, 15));

        assertThatThrownBy(() -> service.allocate(10L, allocate(1, "100.00")))
                .isInstanceOf(BadRequestException.class);
        verify(allocationRepository, never()).save(any());
    }

    @Test
    void refusesAnApplicationDatedBeforeThePayment() {
        AllocateDownpaymentRequest request = allocate(1, "100.00");
        request.setAllocationDate(LocalDate.of(2026, 8, 31));

        assertThatThrownBy(() -> service.allocate(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("before its payment date");
    }

    @Test
    void applicationThenRefundClosesTheDownpayment() {
        service.allocate(10L, allocate(1, "1000.00"));

        RefundDownpaymentRequest refund = new RefundDownpaymentRequest();
        refund.setAmount(new BigDecimal("2000.00"));
        refund.setRefundDate(LocalDate.of(2026, 9, 20));
        DownpaymentResponse response = service.refund(10L, refund);

        assertThat(response.getRefundedAmount()).isEqualByComparingTo("2000.00");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("0");
        assertThat(response.getStatus()).isEqualTo(DownpaymentStatus.CLOSED);
    }

    @Test
    void refusesARefundAboveTheAvailableBalance() {
        RefundDownpaymentRequest refund = new RefundDownpaymentRequest();
        refund.setAmount(new BigDecimal("3000.01"));

        assertThatThrownBy(() -> service.refund(10L, refund))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds the available balance");
    }

    @Test
    void reversingAnApplicationRestoresTheBalanceAndKeepsTheHistory() {
        service.allocate(10L, allocate(1, "1000.00"));
        DownpaymentAllocation allocation = savedAllocations.get(0);
        allocation.setDownpayment(dp);
        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocation));

        DownpaymentResponse response = service.reverseAllocation(10L, 100L, "Wrong invoice");

        assertThat(response.getAvailableBalance()).isEqualByComparingTo("3000.00");
        assertThat(response.getStatus()).isEqualTo(DownpaymentStatus.OPEN);
        assertThat(allocation.getReversed()).isTrue();
        assertThat(savedAllocations).hasSize(1);
        verify(settlementService).reverse(SettlementSourceType.DOWNPAYMENT, 100L);
    }

    @Test
    void cannotReverseADownpaymentThatHasBeenApplied() {
        service.allocate(10L, allocate(1, "1000.00"));

        assertThatThrownBy(() -> service.reverse(10L, "Mistake"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reverse those entries first");
    }

    @Test
    void cannotDeleteAPostedDownpayment() {
        assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be deleted");
    }
}
