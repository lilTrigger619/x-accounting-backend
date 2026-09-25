package com.unionsg.xaccounting.repository.prepayment;

import com.unionsg.xaccounting.entity.prepayment.PrepaymentAmortizationLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrepaymentAmortizationLineRepository extends JpaRepository<PrepaymentAmortizationLine, Long> {
}
