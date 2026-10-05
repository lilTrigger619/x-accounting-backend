package com.unionsg.xaccounting.repository.downpayment;

import com.unionsg.xaccounting.entity.downpayment.DownpaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DownpaymentRefundRepository extends JpaRepository<DownpaymentRefund, Long> {

    List<DownpaymentRefund> findByDownpaymentIdOrderByRefundDateAscIdAsc(Long downpaymentId);

    long countByDownpaymentId(Long downpaymentId);
}
