package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** An organizational department, used to group employees for payroll cost reporting (§34). */
@Entity
@Table(name = "payroll_departments")
@Getter
@Setter
public class Department extends BaseEntity {

    private String name;

    /** Cost-center code shown on department-level payroll cost reports. */
    private String costCenterCode;

    private String description;

    private boolean active = true;
}
