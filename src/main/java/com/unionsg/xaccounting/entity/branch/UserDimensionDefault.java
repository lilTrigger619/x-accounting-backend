package com.unionsg.xaccounting.entity.branch;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** The cost center a user's postings land in when the document doesn't name one. */
@Entity
@Table(name = "user_dimension_defaults")
@Getter
@Setter
public class UserDimensionDefault extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;
}
