package com.unionsg.xaccounting.entity.customer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tax_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "tax_exempt", nullable = false)
    private Boolean taxExempt;

    @Column(name = "tax_exempt_reason")
    private String taxExemptReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }
}
