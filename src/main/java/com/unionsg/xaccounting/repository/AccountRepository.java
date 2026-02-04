package com.unionsg.xaccounting.repository;



import com.unionsg.xaccounting.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
/*
public class AccountRepository {
}
 */

public interface AccountRepository extends JpaRepository<AccountEntity, Long>{
    Optional<AccountEntity> findByAccountId(String accountId);
    boolean existsByAccountId(String accountId);
}
