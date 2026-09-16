package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payroll_work_locations")
@Getter
@Setter
public class WorkLocation extends BaseEntity {

    private String name;

    private String address;

    private boolean active = true;
}
