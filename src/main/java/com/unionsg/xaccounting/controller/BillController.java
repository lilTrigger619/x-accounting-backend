package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.bill.BillResponse;
import com.unionsg.xaccounting.dto.bill.BillTotalsResponse;
import com.unionsg.xaccounting.dto.bill.CreateBillRequest;
import com.unionsg.xaccounting.dto.bill.UpdateBillRequest;
import com.unionsg.xaccounting.service.bill.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    /*
     =============================
     Create Bill
     =============================
     */

    @PostMapping
    public ResponseEntity<BillResponse> create(
            @RequestBody CreateBillRequest request
    ) {
        return ResponseEntity.ok(
                billService.createBill(request)
        );
    }


    /*
     =============================
     Update
     =============================
     */

    @PutMapping("/{billId}")
    public ResponseEntity<BillResponse> update(
            @PathVariable Long billId,
            @Valid @RequestBody UpdateBillRequest request
    ) {
        return ResponseEntity.ok(
                billService.updateBill(billId, request)
        );
    }


    /*
     =============================
     Get By Id
     =============================
     */

    @GetMapping("/{id}")
    public ResponseEntity<BillResponse> get(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                billService.getBill(id)
        );
    }


    /*
     =============================
     List
     =============================
     */

    @GetMapping
    public ResponseEntity<Page<BillResponse>> list(
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                billService.getBills(pageable)
        );
    }


    /*
     =============================
     Outstanding Bills for a Supplier (for payment allocation)
     =============================
     */

    @GetMapping("/outstanding")
    public ResponseEntity<java.util.List<BillResponse>> outstandingForSupplier(
            @RequestParam Long supplierId
    ) {
        return ResponseEntity.ok(
                billService.getOutstandingBillsForSupplier(supplierId)
        );
    }


    /*
     =============================
     Delete (only DRAFT)
     =============================
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        billService.deleteBill(id);
        return ResponseEntity.noContent().build();
    }


    /*
     =============================
     Approve (DRAFT -> OPEN, posts GL journal)
     =============================
     */

    @PostMapping("/{id}/approve")
    public ResponseEntity<BillResponse> approve(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                billService.approveBill(id)
        );
    }


    /*
     =============================
     Mark Paid
     =============================
     */

    @PostMapping("/{id}/paid")
    public ResponseEntity<BillResponse> markPaid(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                billService.markAsPaid(id)
        );
    }


    /*
     =============================
     Cancel
     =============================
     */

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BillResponse> cancel(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                billService.cancelBill(id)
        );
    }

    /*
     =============================
     Bill Totals (Aggregations)
     =============================
     */
    @GetMapping("/summary")
    public ResponseEntity<BillTotalsResponse> summary() {
        return ResponseEntity.ok(billService.getBillTotals());
    }

}
