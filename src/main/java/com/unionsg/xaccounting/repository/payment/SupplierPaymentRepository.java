package com.unionsg.xaccounting.repository.payment;

import com.unionsg.xaccounting.entity.payment.SupplierPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPaymentEntity, Long>, JpaSpecificationExecutor<SupplierPaymentEntity> {
}
