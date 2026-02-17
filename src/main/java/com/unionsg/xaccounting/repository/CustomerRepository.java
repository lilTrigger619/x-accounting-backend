package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>{
    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByDisplayName(String displayName);

    boolean existsByCustomerCode(String customerCode);
}
