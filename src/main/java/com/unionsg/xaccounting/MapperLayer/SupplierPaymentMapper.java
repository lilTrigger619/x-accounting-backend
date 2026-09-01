package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.supplierpayment.*;
import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentEntity;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class SupplierPaymentMapper {

    private SupplierPaymentMapper() {
        throw new UnsupportedOperationException("Mapper class cannot be instantiated");
    }

    public static SupplierPaymentEntity toEntity(
            CreateSupplierPaymentRequest request,
            Supplier supplier,
            ChartOfAccount bankAccount
    ) {
        SupplierPaymentEntity payment = new SupplierPaymentEntity();

        payment.setSupplier(supplier);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setBankAccount(bankAccount);
        payment.setCurrency(request.getCurrency());
        payment.setExchangeRate(request.getExchangeRate() != null
                ? request.getExchangeRate()
                : BigDecimal.ONE);
        payment.setAmountPaid(request.getAmountPaid());
        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setUnallocatedAmount(request.getAmountPaid());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setMemo(request.getMemo());
        payment.setStatus(SupplierPaymentStatus.DRAFT);
        payment.setFullyAllocated(false);

        if (request.getAttachmentIds() != null) {
            payment.setAttachments(request.getAttachmentIds());
        }

        return payment;
    }

    public static SupplierPaymentDetailsResponse toDetailsResponse(
            SupplierPaymentEntity payment,
            List<SupplierPaymentAllocationResponse> allocationResponses
    ) {
        SupplierPaymentDetailsResponse response = new SupplierPaymentDetailsResponse();

        response.setId(payment.getId());
        response.setPaymentNumber(payment.getPaymentNumber());

        if (payment.getSupplier() != null) {
            response.setSupplierId(payment.getSupplier().getId());
            response.setSupplierName(payment.getSupplier().getDisplayName());
            response.setSupplierCode(payment.getSupplier().getSupplierCode());
            response.setSupplierPhone(payment.getSupplier().getPhone());
            response.setSupplierEmail(payment.getSupplier().getEmail());
        }

        response.setPaymentDate(payment.getPaymentDate());
        response.setPaymentMethod(payment.getPaymentMethod());

        if (payment.getBankAccount() != null) {
            response.setBankAccountId(payment.getBankAccount().getId());
            response.setBankAccountName(payment.getBankAccount().getCoa_description());
        }

        response.setCurrency(payment.getCurrency());
        response.setExchangeRate(payment.getExchangeRate());
        response.setAmountPaid(payment.getAmountPaid());
        response.setAllocatedAmount(payment.getAllocatedAmount());
        response.setUnallocatedAmount(payment.getUnallocatedAmount());
        response.setReferenceNumber(payment.getReferenceNumber());
        response.setMemo(payment.getMemo());
        response.setStatus(payment.getStatus());
        response.setAttachmentIds(payment.getAttachments());
        response.setAllocations(allocationResponses != null ? allocationResponses : Collections.emptyList());

        return response;
    }

    public static SupplierPaymentListItemResponse toListItemResponse(SupplierPaymentEntity payment) {
        SupplierPaymentListItemResponse response = new SupplierPaymentListItemResponse();

        response.setId(payment.getId());
        response.setPaymentNumber(payment.getPaymentNumber());

        if (payment.getSupplier() != null) {
            response.setSupplierId(payment.getSupplier().getId());
            response.setSupplierName(payment.getSupplier().getDisplayName());
        }

        response.setPaymentDate(payment.getPaymentDate());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setAmountPaid(payment.getAmountPaid());
        response.setAllocatedAmount(payment.getAllocatedAmount());
        response.setUnallocatedAmount(payment.getUnallocatedAmount());
        response.setStatus(payment.getStatus());

        return response;
    }

    public static CreateSupplierPaymentResponse toCreateResponse(SupplierPaymentEntity payment, String message) {
        CreateSupplierPaymentResponse response = new CreateSupplierPaymentResponse();

        response.setPaymentId(payment.getId());
        response.setPaymentNumber(payment.getPaymentNumber());
        response.setStatus(payment.getStatus());
        response.setMessage(message);

        return response;
    }

    public static SupplierPaymentAllocationResponse toAllocationResponse(
            SupplierPaymentAllocationEntity allocation,
            BigDecimal outstandingBefore
    ) {
        SupplierPaymentAllocationResponse response = new SupplierPaymentAllocationResponse();

        response.setAllocationId(allocation.getId());
        response.setBillId(allocation.getBill().getId());
        response.setBillNumber(allocation.getBill().getBillNumber());
        response.setBillDate(allocation.getBill().getBillDate());
        response.setBillTotal(allocation.getBill().getTotalAmount());
        response.setOutstandingBefore(outstandingBefore);
        response.setAllocatedAmount(allocation.getAllocatedAmount());
        response.setOutstandingAfter(allocation.getBill().getBalance());

        return response;
    }
}
