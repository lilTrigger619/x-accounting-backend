package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.PayFrequency;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Groups employees who share the same payroll cadence and rules (§4) - e.g. "Monthly Salaried
 * Staff", "Weekly Hourly Workers", "Contractors". Every Payroll Calendar Period and Payroll Run
 * belongs to exactly one Payroll Group.
 */
@Entity
@Table(name = "payroll_groups")
@Getter
@Setter
public class PayrollGroup extends BaseEntity {

    private String name;

    @Enumerated(EnumType.STRING)
    private PayFrequency payFrequency;

    private String description;

    private boolean active = true;
}
