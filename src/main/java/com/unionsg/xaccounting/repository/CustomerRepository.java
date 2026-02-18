package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByDisplayName(String displayName);

    boolean existsByCustomerCode(String customerCode);

    Page<Customer> findByDisplayNameContainingIgnoreCase(String displayName, Pageable pageable);

    Page <Customer> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page <Customer> findByPhoneContainingIgnoreCase(String phone, Pageable pageable);
}
