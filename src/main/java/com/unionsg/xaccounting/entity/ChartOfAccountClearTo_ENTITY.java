package com.unionsg.xaccounting.entity;
import lombok.*;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

@Entity
@Table(name="chart_of_account_clear_to")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartOfAccountClearTo_ENTITY {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@Column(columnDefinition="serial")
    private Long id;
//
//    @OneToMany(mappedBy = 'chartOfAccount', cascade = CascadeType.ALL)
//    private List<ChartOfAccount> accounts;

//    @ManyToMany
//    @JoinTable(
//            name = "accounts",
//            joinColumns = @JoinColumn(name="id"),
//            inverseJoinColumns = @JoinColumn(name = "id")
//    )
//    private Set<AccountEntity> accounts =  new HashSet<>();

    // there should be a function to be creating the codes
    // maybe by incrementing or by generating random codes.
    @Column(name = "clear_to_code")
    private Long clearToCode;

    @OneToMany(mappedBy = "coaClearTo", cascade = CascadeType.ALL)
    private List<AccountEntity> accounts;

    @ManyToOne(fetch = FetchType.LAZY) // this is the account type on the frontend
    @JoinColumn(name = "chart_code", referencedColumnName = "coa_code")
    private ChartOfAccount chartOfAccount;

    @Column(name = "description")
    private String description;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_by", nullable = true)
    private String deletedB;

    @Column(name = "date_deleted")
    private LocalDateTime dateDeleted;

    @PrePersist
    protected void onCreate() {
        if (dateCreated == null)
            dateCreated = LocalDateTime.now();
    }
}
