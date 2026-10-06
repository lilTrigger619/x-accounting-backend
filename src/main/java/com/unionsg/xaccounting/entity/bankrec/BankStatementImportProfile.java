package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.bankrec.AmountSignConvention;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A saved CSV column mapping for one bank's statement layout. Each column field holds either a
 * header name (matched case-insensitively) or a 1-based column number.
 */
@Entity
@Table(name = "bank_statement_import_profiles")
@Getter
@Setter
public class BankStatementImportProfile extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(nullable = false, length = 5)
    private String delimiter = ",";

    @Column(name = "has_header_row", nullable = false)
    private Boolean hasHeaderRow = true;

    @Column(name = "skip_rows", nullable = false)
    private Integer skipRows = 0;

    /** java.time pattern, e.g. dd/MM/yyyy. Blank tries the common formats. */
    @Column(name = "date_format", length = 40)
    private String dateFormat;

    @Column(name = "transaction_date_column", nullable = false, length = 100)
    private String transactionDateColumn;

    @Column(name = "value_date_column", length = 100)
    private String valueDateColumn;

    @Column(name = "description_column", length = 100)
    private String descriptionColumn;

    @Column(name = "reference_column", length = 100)
    private String referenceColumn;

    @Column(name = "debit_column", length = 100)
    private String debitColumn;

    @Column(name = "credit_column", length = 100)
    private String creditColumn;

    @Column(name = "amount_column", length = 100)
    private String amountColumn;

    @Column(name = "balance_column", length = 100)
    private String balanceColumn;

    @Column(name = "external_id_column", length = 100)
    private String externalIdColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_sign_convention", nullable = false, length = 30)
    private AmountSignConvention amountSignConvention = AmountSignConvention.POSITIVE_IS_CREDIT;

    @Column(nullable = false)
    private Boolean active = true;
}
