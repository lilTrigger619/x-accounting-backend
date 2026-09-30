package com.unionsg.xaccounting.entity.settings;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * One menu item a user has pinned to their own "Quick Access" section of the sidebar. Purely a
 * per-user shortcut to an existing route - it carries no permissions of its own, so pinning a
 * page a user can't otherwise reach still won't let them reach it (the route's own guard is
 * unaffected). {@code user} is the item's owner and is who every query filters by; it is a
 * separate field from {@code BaseEntity.createdBy} (which is fixed at creation and meant for
 * audit trails) so a future admin-assigned default quick access list remains possible without
 * conflating "who created this row" with "whose sidebar it appears in".
 */
@Entity
@Table(
        name = "quick_access_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_quick_access_user_path", columnNames = {"user_id", "path"})
)
@Getter
@Setter
public class QuickAccessItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(nullable = false, length = 300)
    private String path;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
