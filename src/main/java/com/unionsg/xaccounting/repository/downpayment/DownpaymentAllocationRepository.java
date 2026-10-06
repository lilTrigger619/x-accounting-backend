package com.unionsg.xaccounting.repository.downpayment;

import com.unionsg.xaccounting.entity.downpayment.DownpaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DownpaymentAllocationRepository extends JpaRepository<DownpaymentAllocation, Long> {

    List<DownpaymentAllocation> findByDownpaymentIdOrderByAllocationDateAscIdAsc(Long downpaymentId);

    List<DownpaymentAllocation> findByInvoiceIdOrderByAllocationDateAscIdAsc(Long invoiceId);

    List<DownpaymentAllocation> findByBillIdOrderByAllocationDateAscIdAsc(Long billId);

    List<DownpaymentAllocation> findAllByOrderByAllocationDateAscIdAsc();
}
