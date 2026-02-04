package com.unionsg.xaccounting.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.unionsg.xaccounting.entity.ChartOfAccount;

//public class ChartOfAccountRepository {
//}

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long>{
    /**j
    Optional<ChartOfAccount> findByCoaCode(Long coaCode);
    boolean existsByCoaCode(Long coaCode);
     **/
    Optional <ChartOfAccount> findByCoaCode(Long coaCode);
    boolean existsByCoaCode(Long coaCode);
}
