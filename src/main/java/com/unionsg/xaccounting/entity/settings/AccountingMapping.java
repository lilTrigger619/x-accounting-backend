package com.unionsg.xaccounting.entity.settings;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One row per {@link MappingKey}: the GL account code an automated posting engine (invoice,
 * bill, payment, payroll, closing) resolves at transaction time. Editing this table retargets
 * future postings only - every journal line already stores the resolved account it posted to,
 * so changing a mapping never rewrites a historical transaction (Settings & Setup §40/§41).
 */
@Getter
@Setter
@Entity
@Table(name = "accounting_mappings", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "mapping_key"))
public class AccountingMapping extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_key", nullable = false, length = 60)
    private MappingKey mappingKey;

    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;
}
