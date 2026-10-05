package com.unionsg.xaccounting.repository.banking;

import com.unionsg.xaccounting.entity.banking.BankTransferActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankTransferActivityRepository extends JpaRepository<BankTransferActivity, Long> {

    List<BankTransferActivity> findByBankTransferIdOrderByCreatedAtDescIdDesc(Long bankTransferId);
}
