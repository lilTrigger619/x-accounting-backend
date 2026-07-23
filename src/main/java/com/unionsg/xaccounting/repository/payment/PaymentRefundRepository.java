package com.unionsg.xaccounting.repository.payment;

import com.unionsg.xaccounting.entity.payment.PaymentRefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefundEntity, Long> {
}

