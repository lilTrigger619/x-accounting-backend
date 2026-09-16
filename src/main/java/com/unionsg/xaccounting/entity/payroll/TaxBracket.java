package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** One progressive bracket of a TaxConfiguration: income between min and max taxed at rate. */
@Entity
@Table(name = "tax_brackets")
@Getter
@Setter
public class TaxBracket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_configuration_id", nullable = false)
    private TaxConfiguration taxConfiguration;

    private Integer lineNumber;

    private BigDecimal minIncome;

    /** Null means no upper bound - the top bracket. */
    private BigDecimal maxIncome;

    private BigDecimal ratePercent;
}
