package com.unionsg.xaccounting.repository.banking;

import com.unionsg.xaccounting.entity.banking.BankTransfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BankTransferRepository extends JpaRepository<BankTransfer, Long>, JpaSpecificationExecutor<BankTransfer> {

    /**
     * Row lock taken by post/reverse/cancel so two requests for the same transfer run one after
     * the other; the second then sees the first one's status and is refused.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM BankTransfer t WHERE t.id = :id AND t.deleted = false")
    Optional<BankTransfer> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT t FROM BankTransfer t WHERE t.id = :id AND t.deleted = false")
    Optional<BankTransfer> findActiveById(@Param("id") Long id);
}
