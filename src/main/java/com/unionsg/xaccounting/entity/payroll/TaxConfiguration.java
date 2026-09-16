package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A versioned, jurisdiction-specific income tax configuration (§10) - a progressive set of
 * TaxBrackets plus the liability account employee tax withholdings are credited to. Versioned by
 * effective date so a later change in tax law never retroactively changes an already-posted
 * payroll run's calculation.
 */
@Entity
@Table(name = "tax_configurations")
@Getter
@Setter
public class TaxConfiguration extends BaseEntity {

    private String name;

    private String jurisdiction;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private boolean active = true;

    /** Liability credited when employee income tax is withheld under this configuration. */
    private String taxPayableAccountCode;

    @OneToMany(mappedBy = "taxConfiguration", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<TaxBracket> brackets = new ArrayList<>();
}
