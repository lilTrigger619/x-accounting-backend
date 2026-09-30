package com.unionsg.xaccounting.repository.settings;

import com.unionsg.xaccounting.entity.settings.QuickAccessItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuickAccessItemRepository extends JpaRepository<QuickAccessItem, Long> {
    List<QuickAccessItem> findByUserIdOrderBySortOrderAsc(UUID userId);
    boolean existsByUserIdAndPath(UUID userId, String path);
    Optional<QuickAccessItem> findByIdAndUserId(Long id, UUID userId);
    long countByUserId(UUID userId);
}
