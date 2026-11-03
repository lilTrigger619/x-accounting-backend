package com.unionsg.xaccounting.repository.branch;

import com.unionsg.xaccounting.entity.branch.UserDimensionDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserDimensionDefaultRepository extends JpaRepository<UserDimensionDefault, Long> {

    @Query("SELECT d FROM UserDimensionDefault d JOIN FETCH d.costCenter cc JOIN FETCH cc.branch WHERE d.user.id = :userId")
    Optional<UserDimensionDefault> findForUser(@Param("userId") UUID userId);

    boolean existsByCostCenterId(Long costCenterId);
}
