package com.unionsg.xaccounting.entity.branch;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A physical location of the business (Head Office, Accra, Kumasi...). Every journal line
 * belongs to one branch (through its cost center), and with self-balancing branches switched
 * on every journal must balance per branch, so each branch can produce its own trial balance
 * and balance sheet. Exactly one branch is the Head Office, which existing data is backfilled
 * into.
 */
@Entity
@Table(name = "branches", indexes = {
        @Index(name = "idx_branch_code", columnList = "code")
})
@Getter
@Setter
public class Branch extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 300)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(name = "is_head_office", nullable = false)
    private Boolean headOffice = false;

    @Column(nullable = false)
    private Boolean active = true;
}
