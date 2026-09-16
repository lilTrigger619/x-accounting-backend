package com.unionsg.xaccounting.entity.settings;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A bank (or cash) account the business actually holds, tied to its Chart of Accounts GL
 * account (Settings & Setup §16). Payments/Banking screens pick from this list instead of a
 * hard-coded GL account code, so the app can support more than one bank account without a
 * code change.
 */
@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
public class BankAccount extends BaseEntity {

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "account_number", length = 60)
    private String accountNumber;

    @Column(length = 10)
    private String currency;

    /** The Chart of Accounts code (AccountEntity.accountId) this bank account posts to. */
    @Column(name = "gl_account_code", nullable = false, length = 20)
    private String glAccountCode;

    @Column(length = 100)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BankAccountStatus status = BankAccountStatus.ACTIVE;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "enable_reconciliation")
    private Boolean enableReconciliation = true;
}
