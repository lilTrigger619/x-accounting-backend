package com.unionsg.xaccounting.entity.branch;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Where a user may apply their roles. A row with no branch is Global (every branch and cost
 * center, including ones created later). A row with a branch covers either every cost center
 * of that branch ({@link #allCostCenters}, which also picks up new ones) or only the ticked
 * {@link #costCenters}. A user can hold several branch rows.
 */
@Entity
@Table(name = "user_access_scopes")
@Getter
@Setter
public class UserAccessScope extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Null means Global. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "all_cost_centers", nullable = false)
    private Boolean allCostCenters = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_access_scope_cost_centers",
            joinColumns = @JoinColumn(name = "scope_id"),
            inverseJoinColumns = @JoinColumn(name = "cost_center_id")
    )
    private Set<CostCenter> costCenters = new HashSet<>();

    public boolean isGlobal() {
        return branch == null;
    }
}
