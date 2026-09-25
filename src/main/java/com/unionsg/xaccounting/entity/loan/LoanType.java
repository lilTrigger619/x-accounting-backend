package com.unionsg.xaccounting.entity.loan;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Configurable category of loan (Loans spec §14, e.g. Bank Loan, Employee Loan, Shareholder
 * Loan). {@code defaultDirection} is only a UI hint for the create form - the {@link Loan}
 * record itself always carries its own explicit {@code direction}.
 */
@Entity
@Table(name = "loan_types")
@Getter
@Setter
public class LoanType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_direction", length = 20)
    private LoanDirection defaultDirection;

    @Column(nullable = false)
    private Boolean active = true;
}
