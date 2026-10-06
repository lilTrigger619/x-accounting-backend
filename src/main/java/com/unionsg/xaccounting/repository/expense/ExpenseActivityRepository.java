package com.unionsg.xaccounting.repository.expense;

import com.unionsg.xaccounting.entity.expense.ExpenseActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseActivityRepository extends JpaRepository<ExpenseActivity, Long> {

    List<ExpenseActivity> findByExpenseIdOrderByCreatedAtDescIdDesc(Long expenseId);
}
