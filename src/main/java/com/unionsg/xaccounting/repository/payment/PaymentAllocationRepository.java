package com.unionsg.xaccounting.repository.payment;

import com.unionsg.xaccounting.entity.payment.PaymentAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocationEntity, Long> {

    List<PaymentAllocationEntity> findByPaymentId(Long paymentId);
}
