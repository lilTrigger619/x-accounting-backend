package com.unionsg.xaccounting.entity.configuration;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "config_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String code;

    private String value;

    private String description;

    private Boolean isDefault = false;

//    private String status = "ACTIVE";

    private String status;

    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private Config config;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        if (this.status == null)
            this.status = "ACTIVE";
    }

}