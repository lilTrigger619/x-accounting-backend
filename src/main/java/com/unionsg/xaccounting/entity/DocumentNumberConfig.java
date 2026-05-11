package com.unionsg.xaccounting.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "document_number_config",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "module",
                                "company_id",
                                "branch_id"
                        }
                )
        }
)
@Getter
@Setter
public class DocumentNumberConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String module;

    private String prefix;

    private Long lastNumber;

    private Integer padding;

    private Boolean includeYear;

    private Boolean includeMonth;

    private Boolean resetYearly;

    private Boolean resetMonthly;

    private String separator;

    private Long companyId;

    private Long branchId;

    private Integer lastResetYear;

    private Integer lastResetMonth;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

   public DocumentNumberConfig(){
        this.resetMonthly = false;
   }
};