package com.unionsg.xaccounting.repository.downpayment;

import com.unionsg.xaccounting.entity.downpayment.Downpayment;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface DownpaymentRepository extends JpaRepository<Downpayment, Long>, JpaSpecificationExecutor<Downpayment> {

    List<Downpayment> findByTypeAndDeletedFalseAndStatusIn(DownpaymentType type, Collection<DownpaymentStatus> statuses);

    List<Downpayment> findByTypeAndDeletedFalse(DownpaymentType type);

    List<Downpayment> findByDeletedFalse();
}
