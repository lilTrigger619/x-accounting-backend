package com.unionsg.xaccounting.repository.deposit;

import com.unionsg.xaccounting.entity.deposit.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DepositRepository extends JpaRepository<Deposit, Long>, JpaSpecificationExecutor<Deposit> {

    boolean existsByDepositTypeId(Long depositTypeId);

    List<Deposit> findByDeletedFalse();

    List<Deposit> findByCustomerIdAndDeletedFalseOrderByDepositDateAscIdAsc(Long customerId);

    List<Deposit> findBySupplierIdAndDeletedFalseOrderByDepositDateAscIdAsc(Long supplierId);

    List<Deposit> findByEmployeeIdAndDeletedFalseOrderByDepositDateAscIdAsc(Long employeeId);

    List<Deposit> findByCounterpartyTypeAndCounterpartyNameIgnoreCaseAndDeletedFalseOrderByDepositDateAscIdAsc(
            com.unionsg.xaccounting.enums.deposit.DepositCounterpartyType type, String name);
}
