package com.unionsg.xaccounting.repository.payment;

import com.unionsg.xaccounting.entity.payment.SupplierPaymentAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPaymentAllocationRepository extends JpaRepository<SupplierPaymentAllocationEntity, Long> {

    List<SupplierPaymentAllocationEntity> findBySupplierPaymentId(Long supplierPaymentId);

    List<SupplierPaymentAllocationEntity> findByBillId(Long billId);

    List<SupplierPaymentAllocationEntity> findByBill_SupplierId(Long supplierId);
}
