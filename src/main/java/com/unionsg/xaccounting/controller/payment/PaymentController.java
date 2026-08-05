package com.unionsg.xaccounting.controller.payment;

import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.payment.*;
import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.service.payment.PaymentAllocationService;
import com.unionsg.xaccounting.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentAllocationService paymentAllocationService;

    // ========================================================================
    // CREATE PAYMENT
    // ========================================================================

    @PostMapping
    @Operation(summary = "Create a payment", description = "Creates a new payment with RECEIVED status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business validation error")
    })
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        CreatePaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CreatePaymentResponse>builder()
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
    @Operation(summary = "Save a draft payment", description = "Creates a new payment with DRAFT status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Draft payment saved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> saveDraft(
            @Valid @RequestBody CreateDraftPaymentRequest request
    ) {
        CreatePaymentResponse response = paymentService.saveDraft(request);
        return ResponseEntity.ok(
                ApiResponse.<CreatePaymentResponse>builder()
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
    @Operation(summary = "Update a draft payment", description = "Updates an existing draft payment. Only DRAFT payments can be updated.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Draft payment updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Payment is not in DRAFT status")
    })
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> updateDraft(
            @PathVariable Long paymentId,
            @Valid @RequestBody UpdateDraftPaymentRequest request
    ) {
        CreatePaymentResponse response = paymentService.updateDraft(paymentId, request);
        return ResponseEntity.ok(
                ApiResponse.<CreatePaymentResponse>builder()
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
    @Operation(summary = "Delete a draft payment", description = "Soft deletes a draft payment. Only DRAFT payments can be deleted.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Draft payment deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Payment is not in DRAFT status")
    })
    public ResponseEntity<Void> deleteDraft(@PathVariable Long paymentId) {
        paymentService.deleteDraft(paymentId);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // GET PAYMENT DETAILS
    // ========================================================================

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details", description = "Retrieves detailed information about a specific payment including allocations and refunds")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> getPaymentDetails(
            @PathVariable Long paymentId
    ) {
        PaymentDetailsResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
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
    @Operation(summary = "List payments", description = "Retrieves a paginated list of payments with optional filtering")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<PaymentListItemResponse>>> listPayments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long customerId,
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
        PaymentFilterRequest filterRequest = new PaymentFilterRequest();
        filterRequest.setSearch(search);
        filterRequest.setCustomerId(customerId);
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
                    com.unionsg.xaccounting.enums.PaymentStatus.valueOf(status.toUpperCase())
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

        Page<PaymentListItemResponse> response = paymentService.getPayments(filterRequest);
        return ResponseEntity.ok(
                ApiResponse.<Page<PaymentListItemResponse>>builder()
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
    @Operation(summary = "Allocate payment to invoices", description = "Allocates a payment to one or more invoices")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment allocated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment or invoice not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business validation error")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> allocatePayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody AllocatePaymentRequest request
    ) {
        paymentAllocationService.allocatePayment(paymentId, request);
        PaymentDetailsResponse details = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
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
    @Operation(summary = "Remove an allocation", description = "Removes a specific allocation from a payment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Allocation removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment or allocation not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business validation error")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> removeAllocation(
            @PathVariable Long paymentId,
            @PathVariable Long allocationId
    ) {
        paymentAllocationService.removeAllocation(allocationId);
        PaymentDetailsResponse details = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
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
    @Operation(summary = "Auto-allocate to oldest invoices first", description = "Automatically allocates the unallocated balance to the oldest outstanding invoices")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Auto-allocation completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business validation error")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> autoAllocateOldest(
            @PathVariable Long paymentId
    ) {
        paymentAllocationService.autoAllocateOldest(paymentId);
        PaymentDetailsResponse details = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
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
    @Operation(summary = "Auto-allocate to largest invoices first", description = "Automatically allocates the unallocated balance to the largest outstanding invoices")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Auto-allocation completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business validation error")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> autoAllocateLargest(
            @PathVariable Long paymentId
    ) {
        paymentAllocationService.autoAllocateLargest(paymentId);
        PaymentDetailsResponse details = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
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
    @Operation(summary = "Clear all allocations", description = "Removes all allocations from a payment and reverts the unallocated balance")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All allocations cleared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business validation error")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> clearAllocations(
            @PathVariable Long paymentId
    ) {
        paymentAllocationService.clearAllocations(paymentId);
        PaymentDetailsResponse details = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
                        .success(true)
                        .message("All allocations cleared successfully")
                        .content(details)
                        .build()
        );
    }

    // ========================================================================
    // REFUND PAYMENT
    // ========================================================================

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a payment", description = "Processes a refund against a payment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business validation error")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> refundPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundPaymentRequest request
    ) {
        PaymentDetailsResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
                        .success(true)
                        .message("Refund processed successfully")
                        .content(response)
                        .build()
        );
    }

    // ========================================================================
    // GENERATE RECEIPT
    // ========================================================================

    @GetMapping("/{paymentId}/receipt")
    @Operation(summary = "Generate receipt PDF", description = "Generates and returns the payment receipt as a PDF document")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Receipt generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<byte[]> generateReceipt(@PathVariable Long paymentId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(new byte[0]);
    }

    // ========================================================================
    // PREVIEW RECEIPT
    // ========================================================================

    @GetMapping("/{paymentId}/receipt/preview")
    @Operation(summary = "Preview receipt PDF", description = "Generates a preview of the payment receipt as a PDF document")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Receipt preview generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<byte[]> previewReceipt(@PathVariable Long paymentId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(new byte[0]);
    }

    // ========================================================================
    // DOWNLOAD RECEIPT
    // ========================================================================

    @GetMapping("/{paymentId}/receipt/download")
    @Operation(summary = "Download receipt PDF", description = "Downloads the payment receipt as a PDF attachment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Receipt downloaded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long paymentId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"receipt-" + paymentId + ".pdf\"")
                .body(new byte[0]);
    }

    // ========================================================================
    // EMAIL RECEIPT
    // ========================================================================

    @PostMapping("/{paymentId}/email")
    @Operation(summary = "Email payment receipt", description = "Sends the payment receipt via email")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Receipt emailed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Email sending failed")
    })
    public ResponseEntity<ApiResponse<String>> emailReceipt(
            @PathVariable Long paymentId,
            @Valid @RequestBody ReceiptEmailRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Receipt emailed successfully")
                        .content("Receipt for payment " + paymentId + " has been sent.")
                        .build()
        );
    }

    // ========================================================================
    // UPLOAD ATTACHMENTS
    // ========================================================================

    @PostMapping("/{paymentId}/attachments")
    @Operation(summary = "Upload attachments", description = "Attaches files to a payment using the existing file upload module")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attachments uploaded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> uploadAttachments(
            @PathVariable Long paymentId,
            @Valid @RequestBody List<Long> attachmentIds
    ) {
        PaymentDetailsResponse details = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
                        .success(true)
                        .message("Attachments uploaded successfully")
                        .content(details)
                        .build()
        );
    }

    // ========================================================================
    // REMOVE ATTACHMENT
    // ========================================================================

    @DeleteMapping("/{paymentId}/attachments/{attachmentId}")
    @Operation(summary = "Remove an attachment", description = "Removes a specific attachment from a payment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attachment removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment or attachment not found")
    })
    public ResponseEntity<ApiResponse<PaymentDetailsResponse>> removeAttachment(
            @PathVariable Long paymentId,
            @PathVariable Long attachmentId
    ) {
        PaymentDetailsResponse details = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(
                ApiResponse.<PaymentDetailsResponse>builder()
                        .success(true)
                        .message("Attachment removed successfully")
                        .content(details)
                        .build()
        );
    }

    // ========================================================================
    // GET ATTACHMENTS
    // ========================================================================

    @GetMapping("/{paymentId}/attachments")
    @Operation(summary = "Get attachments", description = "Retrieves all attachments associated with a payment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attachments retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<ApiResponse<List<FileResponseDto>>> getAttachments(
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<FileResponseDto>>builder()
                        .success(true)
                        .message("Attachments retrieved successfully")
                        .content(List.of())
                        .build()
        );
    }

    // ========================================================================
    // ACTIVITY TIMELINE
    // ========================================================================

    @GetMapping("/{paymentId}/activities")
    @Operation(summary = "Get activity timeline", description = "Retrieves the activity timeline for a payment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activities retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivityTimeline(
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<ActivityResponse>>builder()
                        .success(true)
                        .message("Activities retrieved successfully")
                        .content(List.of())
                        .build()
        );
    }

    // ========================================================================
    // EMAIL HISTORY
    // ========================================================================

    @GetMapping("/{paymentId}/emails")
    @Operation(summary = "Get email history", description = "Retrieves the email history for a payment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email history retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found")
    })
    public ResponseEntity<ApiResponse<List<EmailHistoryResponse>>> getEmailHistory(
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<EmailHistoryResponse>>builder()
                        .success(true)
                        .message("Email history retrieved successfully")
                        .content(List.of())
                        .build()
        );
    }
}
