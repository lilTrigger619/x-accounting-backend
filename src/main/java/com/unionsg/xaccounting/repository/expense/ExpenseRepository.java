package com.unionsg.xaccounting.repository.expense;

import com.unionsg.xaccounting.entity.expense.Expense;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    /**
     * Row lock taken by update/post/reverse/delete so two requests for the same expense run one
     * after the other; the second then sees the first one's status and is refused.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.deleted = false")
    Optional<Expense> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.deleted = false")
    Optional<Expense> findActiveById(@Param("id") Long id);
}
