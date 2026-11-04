package com.unionsg.xaccounting.entity.branch;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Anything that can incur cost (Engineering, Finance, HR, Cleaning...). Always belongs to one
 * branch, so "Engineering, Accra" and "Engineering, Kumasi" are two cost centers. Every branch
 * owns one default cost center ({@link #branchDefault}) that stands for the branch itself.
 */
@Entity
@Table(name = "cost_centers", indexes = {
        @Index(name = "idx_cost_center_code", columnList = "code"),
        @Index(name = "idx_cost_center_branch", columnList = "branch_id")
})
@Getter
@Setter
public class CostCenter extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "is_branch_default", nullable = false)
    private Boolean branchDefault = false;

    @Column(nullable = false)
    private Boolean active = true;
}
