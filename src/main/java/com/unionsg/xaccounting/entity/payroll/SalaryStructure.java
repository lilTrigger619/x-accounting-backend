package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A named template describing how a category of employee's compensation is composed (§6). */
@Entity
@Table(name = "salary_structures")
@Getter
@Setter
public class SalaryStructure extends BaseEntity {

    private String name;

    private String description;

    private boolean active = true;

    @OneToMany(mappedBy = "salaryStructure", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalaryStructureLine> lines = new ArrayList<>();
}
