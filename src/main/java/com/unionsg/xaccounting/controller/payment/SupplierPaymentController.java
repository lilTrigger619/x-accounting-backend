package com.unionsg.xaccounting.controller.payment;

import com.unionsg.xaccounting.dto.supplierpayment.*;
import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.service.payment.SupplierPaymentAllocationService;
import com.unionsg.xaccounting.service.payment.SupplierPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplier-payments")
@RequiredArgsConstructor
@Tag(name = "Supplier Payments", description = "Accounts Payable payment management endpoints")
public class SupplierPaymentController {

    private final SupplierPaymentService supplierPaymentService;
    private final SupplierPaymentAllocationService supplierPaymentAllocationService;

    // ========================================================================
    // CREATE PAYMENT
    // ========================================================================

    @PostMapping
    @Operation(summary = "Create a supplier payment", description = "Creates a new supplier payment with PAID status and posts its GL journal")
    public ResponseEntity<ApiResponse<CreateSupplierPaymentResponse>> createPayment(
            @Valid @RequestBody CreateSupplierPaymentRequest request
    ) {
        CreateSupplierPaymentResponse response = supplierPaymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CreateSupplierPaymentResponse>builder()
                        .success(true)
                        .message("Payment created successfully")
                        .content(response)
                        .build()
                );
    }

    // ========================================================================
    // SAVE DRAFT
    // ========================================================================

    @PostMapping("/draft")
    @Operation(summary = "Save a draft supplier payment")
    public ResponseEntity<ApiResponse<CreateSupplierPaymentResponse>> saveDraft(
            @Valid @RequestBody CreateSupplierPaymentRequest request
    ) {
        CreateSupplierPaymentResponse response = supplierPaymentService.saveDraft(request);
        return ResponseEntity.ok(
                ApiResponse.<CreateSupplierPaymentResponse>builder()
                        .success(true)
                        .message("Draft payment saved successfully")
                        .content(response)
                        .build()
        );
    }

    // ========================================================================
    // UPDATE DRAFT
    // ========================================================================

    @PutMapping("/{paymentId}")
    @Operation(summary = "Update a draft supplier payment")
    public ResponseEntity<ApiResponse<CreateSupplierPaymentResponse>> updateDraft(
            @PathVariable Long paymentId,
            @Valid @RequestBody CreateSupplierPaymentRequest request
    ) {
        CreateSupplierPaymentResponse response = supplierPaymentService.updateDraft(paymentId, request);
        return ResponseEntity.ok(
                ApiResponse.<CreateSupplierPaymentResponse>builder()
                        .success(true)
                        .message("Draft payment updated successfully")
                        .content(response)
                        .build()
        );
    }

    // ========================================================================
    // DELETE DRAFT
    // ========================================================================

    @DeleteMapping("/{paymentId}")
    @Operation(summary = "Delete a draft supplier payment")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long paymentId) {
        supplierPaymentService.deleteDraft(paymentId);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // GET PAYMENT DETAILS
    // ========================================================================

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get supplier payment details")
    public ResponseEntity<ApiResponse<SupplierPaymentDetailsResponse>> getPaymentDetails(
            @PathVariable Long paymentId
    ) {
        SupplierPaymentDetailsResponse response = supplierPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<SupplierPaymentDetailsResponse>builder()
                        .success(true)
                        .message("Payment details retrieved successfully")
                        .content(response)
                        .build()
        );
    }

    // ========================================================================
    // LIST PAYMENTS
    // ========================================================================

    @GetMapping
    @Operation(summary = "List supplier payments")
    public ResponseEntity<ApiResponse<Page<SupplierPaymentListItemResponse>>> listPayments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        SupplierPaymentFilterRequest filterRequest = new SupplierPaymentFilterRequest();
        filterRequest.setSearch(search);
        filterRequest.setSupplierId(supplierId);
        filterRequest.setPage(page);
        filterRequest.setSize(size);
        filterRequest.setSortBy(sortBy);
        filterRequest.setSortDirection(sortDirection);

        if (paymentMethod != null && !paymentMethod.isBlank()) {
            filterRequest.setPaymentMethod(
                    com.unionsg.xaccounting.enums.PaymentMethod.valueOf(paymentMethod.toUpperCase())
            );
        }
        if (status != null && !status.isBlank()) {
            filterRequest.setStatus(
                    com.unionsg.xaccounting.enums.SupplierPaymentStatus.valueOf(status.toUpperCase())
            );
        }
        if (bankAccountId != null) {
            filterRequest.setBankAccountId(bankAccountId);
        }
        if (fromDate != null && !fromDate.isBlank()) {
            filterRequest.setFromDate(java.time.LocalDate.parse(fromDate));
        }
        if (toDate != null && !toDate.isBlank()) {
            filterRequest.setToDate(java.time.LocalDate.parse(toDate));
        }

        Page<SupplierPaymentListItemResponse> response = supplierPaymentService.getPayments(filterRequest);
        return ResponseEntity.ok(
                ApiResponse.<Page<SupplierPaymentListItemResponse>>builder()
                        .success(true)
                        .message("Payments retrieved successfully")
                        .content(response)
                        .build()
        );
    }

    // ========================================================================
    // ALLOCATE PAYMENT
    // ========================================================================

    @PostMapping("/{paymentId}/allocate")
    @Operation(summary = "Allocate payment to bills")
    public ResponseEntity<ApiResponse<SupplierPaymentDetailsResponse>> allocatePayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody AllocateSupplierPaymentRequest request
    ) {
        supplierPaymentAllocationService.allocatePayment(paymentId, request);
        SupplierPaymentDetailsResponse details = supplierPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<SupplierPaymentDetailsResponse>builder()
                        .success(true)
                        .message("Payment allocated successfully")
                        .content(details)
                        .build()
        );
    }

    // ========================================================================
    // REMOVE ALLOCATION
    // ========================================================================

    @DeleteMapping("/{paymentId}/allocation/{allocationId}")
    @Operation(summary = "Remove an allocation")
    public ResponseEntity<ApiResponse<SupplierPaymentDetailsResponse>> removeAllocation(
            @PathVariable Long paymentId,
            @PathVariable Long allocationId
    ) {
        supplierPaymentAllocationService.removeAllocation(allocationId);
        SupplierPaymentDetailsResponse details = supplierPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<SupplierPaymentDetailsResponse>builder()
                        .success(true)
                        .message("Allocation removed successfully")
                        .content(details)
                        .build()
        );
    }

    // ========================================================================
    // AUTO ALLOCATE - OLDEST FIRST
    // ========================================================================

    @PostMapping("/{paymentId}/allocate/oldest")
    @Operation(summary = "Auto-allocate to oldest bills first")
    public ResponseEntity<ApiResponse<SupplierPaymentDetailsResponse>> autoAllocateOldest(
            @PathVariable Long paymentId
    ) {
        supplierPaymentAllocationService.autoAllocateOldest(paymentId);
        SupplierPaymentDetailsResponse details = supplierPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<SupplierPaymentDetailsResponse>builder()
                        .success(true)
                        .message("Auto-allocation completed successfully")
                        .content(details)
                        .build()
        );
    }

    // ========================================================================
    // AUTO ALLOCATE - LARGEST FIRST
    // ========================================================================

    @PostMapping("/{paymentId}/allocate/largest")
    @Operation(summary = "Auto-allocate to largest bills first")
    public ResponseEntity<ApiResponse<SupplierPaymentDetailsResponse>> autoAllocateLargest(
            @PathVariable Long paymentId
    ) {
        supplierPaymentAllocationService.autoAllocateLargest(paymentId);
        SupplierPaymentDetailsResponse details = supplierPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<SupplierPaymentDetailsResponse>builder()
                        .success(true)
                        .message("Auto-allocation completed successfully")
                        .content(details)
                        .build()
        );
    }

    // ========================================================================
    // CLEAR ALLOCATIONS
    // ========================================================================

    @DeleteMapping("/{paymentId}/allocation")
    @Operation(summary = "Clear all allocations")
    public ResponseEntity<ApiResponse<SupplierPaymentDetailsResponse>> clearAllocations(
            @PathVariable Long paymentId
    ) {
        supplierPaymentAllocationService.clearAllocations(paymentId);
        SupplierPaymentDetailsResponse details = supplierPaymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<SupplierPaymentDetailsResponse>builder()
                        .success(true)
                        .message("All allocations cleared successfully")
                        .content(details)
                        .build()
        );
    }
}
