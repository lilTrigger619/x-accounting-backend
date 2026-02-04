/*

public class Account {
}

 */
package com.unionsg.xaccounting.entity;
import lombok.*;
//import javax.persistence.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "account_name")
    private String accountName;

//    @ManyToMany(mappedBy = "accounts")
//    private Set<ChartOfAccountClearTo_ENTITY> coaClearTo= new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account", referencedColumnName = "id")
    private ChartOfAccountClearTo_ENTITY coaClearTo;
    /**
    @Column(name = "chart_code", length = 150)
    private String chartCode;
     */

//    @ManyToOne(fetch = FetchType.LAZY) // this is the account type on the frontend
//    @JoinColumn(name = "chart_code", referencedColumnName = "coa_code")
//    private ChartOfAccount chartOfAccount;


//    @Column(name = "clears_to", length = 150) // this is a parent account for when closing the account
//    private String clearsTo;
//
//    @Column(length = 150)
//    private String restriction;
//
//    @Column(name = "posting_level", length = 150)
//    private String postingLevel;
////
//    @Column(name = "level_id", length = 150)
//    private String levelId;
//
//    @Column(length = 150)
//    private String currency;
//
//    @Column(name = "statement_type", length = 150)
//    private String statementType;
//
//    @Column(name = "statement_code", length = 150)
//    private String statementCode;
//
//    @Column(name = "society_id")
//    private String societyId;
//
//    @Column(name = "circuit_id")
//    private String circuitId;
//
//    @Column(name = "posted_by", length = 150)
//    private String postedBy;
//
//    @Column(name = "approved_by", length = 150)
//    private String approvedBy;

//    @Column(name = "approved_status", length = 150, columnDefinition = "varchar(150) default '0'")
//    private String approvedStatus;
//
//    @Column(length = 150, columnDefinition = "varchar(150) default '0'")
//    private String deleted



    @Column(name = "deleted")
    private boolean  deleted = false;

    @Column(name = "deletedBy", nullable = false)
    private String deletedBy ;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    //@Column(name = "date_created", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name="created_by")
    private String createdBy;

    @Column(name = "opening_balance")
    private String openingBalance;

    @Column(name = "currency")
    private String currency;

    @Column(name = "opening_balance_date")
    private LocalDateTime opening_balance_date;

    @Column(name = "tax_rate")
    private String taxRate;

    @Column(name="description")
    private String description;

    @PrePersist
    protected void onCreate() {
        if (dateCreated == null) {
            dateCreated = LocalDateTime.now();
        }

    }
}

//@Entity
//@Table(name="sub_account")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class AccountEntity {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name="name")
//    private String
//}