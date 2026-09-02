package com.unionsg.xaccounting.repository.customer;

import com.unionsg.xaccounting.entity.customer.CustomerActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerActivityLogRepository extends JpaRepository<CustomerActivityLog, Long> {

    Page<CustomerActivityLog> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}
