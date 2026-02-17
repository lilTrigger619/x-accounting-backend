package com.unionsg.xaccounting.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

import lombok.*;

@Entity
@Data
@Table(name="Chart_of_account")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartOfAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coa_code", nullable = false, unique = true)
    private Long coaCode;

    @Column(nullable = false)
    private String coa_description;

    @Column(name = "date_created")
    private LocalDate dateCreated;

    // optional; Bidirectional relationship
    @OneToMany(mappedBy = "chartOfAccount", cascade = CascadeType.ALL)
    private List<ChartOfAccountClearTo_ENTITY> coaClearTo;


    @Column(nullable=false)
    private boolean deleted = false;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    // later change this to a user. let this point to a user.
    @Column(nullable = true)
    private String deletedBy;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name= "clear_to", referencedColumnName = 'clear_to')
//    private clearTO chartOfAccount;

}