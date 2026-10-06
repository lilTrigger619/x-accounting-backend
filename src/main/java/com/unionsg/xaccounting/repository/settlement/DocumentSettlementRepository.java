package com.unionsg.xaccounting.repository.settlement;

import com.unionsg.xaccounting.entity.settlement.DocumentSettlement;
import com.unionsg.xaccounting.enums.settlement.SettlementSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentSettlementRepository extends JpaRepository<DocumentSettlement, Long> {

    List<DocumentSettlement> findByInvoiceIdOrderBySettlementDateAscIdAsc(Long invoiceId);

    List<DocumentSettlement> findByBillIdOrderBySettlementDateAscIdAsc(Long billId);

    Optional<DocumentSettlement> findBySourceTypeAndSourceAllocationIdAndReversedFalse(
            SettlementSourceType sourceType, Long sourceAllocationId);

    boolean existsBySourceTypeAndSourceAllocationIdAndReversedFalse(
            SettlementSourceType sourceType, Long sourceAllocationId);
}
