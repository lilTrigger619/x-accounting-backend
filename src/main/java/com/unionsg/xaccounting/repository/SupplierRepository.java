package com.unionsg.xaccounting.repository;

import com.unionsg.xaccounting.entity.supplier.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier>{
    Optional<Supplier> findBySupplierCode(String customerCode);

    boolean existsByDisplayName(String displayName);

    boolean existsBySupplierCode(String customerCode);

    Page <Supplier> findByDisplayNameContainingIgnoreCase(String displayName, Pageable pageable);

    Page <Supplier> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page <Supplier> findByPhoneContainingIgnoreCase(String phone, Pageable pageable);
}
