package com.unionsg.xaccounting.repository.prepayment;

import com.unionsg.xaccounting.entity.prepayment.Prepayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrepaymentRepository extends JpaRepository<Prepayment, Long> {
}
