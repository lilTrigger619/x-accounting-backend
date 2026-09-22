package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.EmploymentStatus;
import com.unionsg.xaccounting.enums.EmploymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * The payroll master record for an employee (§2). Deliberately independent of the application's
 * login {@code User} entity - not every employee needs (or should have) system access, and not
 * every system user is on payroll.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
public class Employee extends BaseEntity {

    @Column(unique = true)
    private String employeeNumber;

    private String firstName;

    private String lastName;

    private String workEmail;

    private String personalEmail;

    private String phone;

    private LocalDate dateOfBirth;

    private String nationalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_location_id")
    private WorkLocation workLocation;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    private LocalDate dateOfEmployment;

    private LocalDate terminationDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_group_id", nullable = false)
    private PayrollGroup payrollGroup;

    // ===== Payment information =====

    private String bankName;

    private String bankAccountName;

    private String bankAccountNumber;

    private String bankRoutingNumber;

    private String currency = "USD";

    // ===== Tax / statutory information =====

    private String taxIdentifier;

    private String statutoryId;

    private String notes;

    private String photoFileId;

    public String getFullName() {
        return (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
    }
}
