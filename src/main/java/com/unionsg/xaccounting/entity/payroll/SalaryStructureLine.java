package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One component within a Salary Structure, with the value this structure applies for it - a
 * fixed amount, or a percentage (of basic or gross, per the component's own calculation method).
 */
@Entity
@Table(name = "salary_structure_lines")
@Getter
@Setter
public class SalaryStructureLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure salaryStructure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pay_component_id", nullable = false)
    private PayComponent payComponent;

    /** A fixed amount, or a percentage number (e.g. 5 = 5%), per the component's calc method. */
    private BigDecimal value;
}
