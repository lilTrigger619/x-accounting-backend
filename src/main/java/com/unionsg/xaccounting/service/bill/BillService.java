package com.unionsg.xaccounting.service.bill;

import com.unionsg.xaccounting.MapperLayer.BillMapper;
import com.unionsg.xaccounting.dto.bill.BillResponse;
import com.unionsg.xaccounting.dto.bill.BillTotalsResponse;
import com.unionsg.xaccounting.dto.bill.BillTotalsRow;
import com.unionsg.xaccounting.dto.bill.CreateBillRequest;
import com.unionsg.xaccounting.dto.bill.UpdateBillRequest;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.service.payment.APJournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final SupplierRepository supplierRepository;
    private final BillCalculationService calculationService;
    private final BillNumberGenerator numberGenerator;
    private final APJournalService apJournalService;

    @Transactional
    public BillResponse createBill(CreateBillRequest request) {

        Supplier supplier =
                supplierRepository.findById(request.getSupplierId())
                        .orElseThrow(() -> new RuntimeException("Supplier not found"));

        Bill bill = BillMapper.toEntity(request, supplier);

        bill.setCreatedAt(LocalDateTime.now());
        bill.setBillNumber(numberGenerator.generateBillNumber());

        calculationService.calculateBill(bill);

        Bill saved = billRepository.save(bill);

        return BillMapper.toResponse(saved);
    }


    @Transactional
    public BillResponse updateBill(Long billId, UpdateBillRequest request) {

        Bill bill =
                billRepository.findById(billId)
                        .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessException("Only draft bills can be modified.");
        }

        Supplier supplier =
                supplierRepository.findById(request.getSupplierId())
                        .orElseThrow(() -> new RuntimeException("Supplier not found"));

        BillMapper.applyUpdate(bill, request, supplier);

        bill.setUpdatedAt(LocalDateTime.now());

        calculationService.calculateBill(bill);

        Bill saved = billRepository.save(bill);

        return BillMapper.toResponse(saved);
    }


    public BillResponse getBill(Long id) {

        Bill bill =
                billRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Bill not found"));

        return BillMapper.toResponse(bill);
    }


    public Page<BillResponse> getBills(Pageable pageable) {

        return billRepository
                .findAll(pageable)
                .map(BillMapper::toResponse);
    }


    /**
     * Bills for a supplier that still carry an outstanding balance (OPEN or
     * PARTIALLY_PAID) — used to populate the supplier-payment allocation screen.
     */
    public java.util.List<BillResponse> getOutstandingBillsForSupplier(Long supplierId) {

        return billRepository.findBySupplierId(supplierId).stream()
                .filter(bill -> (bill.getStatus() == BillStatus.OPEN || bill.getStatus() == BillStatus.PARTIALLY_PAID)
                        && bill.getBalance() != null
                        && bill.getBalance().compareTo(java.math.BigDecimal.ZERO) > 0)
                .map(BillMapper::toResponse)
                .toList();
    }


    @Transactional
    public void deleteBill(Long id) {

        Bill bill =
                billRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessException("Only draft bills can be cancelled.");
        }

        bill.setStatus(BillStatus.CANCELLED);
        bill.setCancelledAt(LocalDateTime.now());

        billRepository.save(bill);
    }


    /**
     * Approves (records) a draft bill: moves it out of DRAFT into OPEN
     * and posts its GL impact (Dr Expense, Cr Accounts Payable).
     */
    @Transactional
    public BillResponse approveBill(Long id) {

        Bill bill =
                billRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessException("Only draft bills can be approved.");
        }

        bill.setStatus(BillStatus.OPEN);
        bill.setApprovedAt(LocalDateTime.now());

        Bill saved = billRepository.save(bill);

        apJournalService.postBillJournal(saved);

        return BillMapper.toResponse(saved);
    }


    @Transactional
    public BillResponse markAsPaid(Long id) {

        Bill bill =
                billRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Bill not found"));

        bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(LocalDateTime.now());

        return BillMapper.toResponse(
                billRepository.save(bill)
        );
    }


    @Transactional
    public BillResponse cancelBill(Long id) {

        Bill bill =
                billRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BusinessException("Only draft bills can be cancelled.");
        }

        bill.setStatus(BillStatus.CANCELLED);
        bill.setCancelledAt(LocalDateTime.now());

        return BillMapper.toResponse(
                billRepository.save(bill)
        );
    }


    @Transactional(readOnly = true)
    public BillTotalsResponse getBillTotals() {

        BillTotalsRow row =
                billRepository.getBillTotals(
                        BillStatus.PAID,
                        BillStatus.OVERDUE,
                        java.util.List.of(
                                BillStatus.OPEN,
                                BillStatus.DRAFT,
                                BillStatus.PARTIALLY_PAID
                        )
                );

        BillTotalsResponse response = new BillTotalsResponse();

        response.setPaid(
                new BillTotalsResponse.SummaryItem(
                        row.getPaidCount(),
                        row.getPaidAmount()
                )
        );

        response.setOverdue(
                new BillTotalsResponse.SummaryItem(
                        row.getOverdueCount(),
                        row.getOverdueAmount()
                )
        );

        response.setPending(
                new BillTotalsResponse.SummaryItem(
                        row.getPendingCount(),
                        row.getPendingAmount()
                )
        );

        response.setGrandTotal(
                new BillTotalsResponse.SummaryItem(
                        row.getGrandCount(),
                        row.getGrandAmount()
                )
        );

        return response;
    }

}
