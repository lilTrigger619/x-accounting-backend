package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** One uploaded bank statement file and what happened to its rows. */
@Entity
@Table(name = "bank_statement_imports")
@Getter
@Setter
public class BankStatementImport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    /** The reconciliation the file was imported from, when it was imported inside one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id")
    private BankReconciliation reconciliation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private BankStatementImportProfile profile;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "first_transaction_date")
    private LocalDate firstTransactionDate;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = 0;

    @Column(name = "imported_rows", nullable = false)
    private Integer importedRows = 0;

    @Column(name = "duplicate_rows", nullable = false)
    private Integer duplicateRows = 0;

    @Column(name = "error_rows", nullable = false)
    private Integer errorRows = 0;

    @Column(name = "imported_by_name", length = 150)
    private String importedByName;
}
