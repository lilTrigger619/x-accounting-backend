package com.unionsg.xaccounting.entity.configuration;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String configKey;

    @Column(nullable = false)
    private String title;

    private String description;

    private String itemLabel;

    @Builder.Default
    private Boolean showValueField = false;

    private String valueFieldLabel;

    private String valueFieldPlaceholder;

    @Builder.Default
    private Boolean systemDefined = true;

//    private String status = "ACTIVE";

    private String status;

    private Integer sortOrder;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    @Builder.Default
    @OneToMany(
            mappedBy = "config",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ConfigItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate(){
       if (this.status == null)
           this.status = "ACTIVE";
    }

}
