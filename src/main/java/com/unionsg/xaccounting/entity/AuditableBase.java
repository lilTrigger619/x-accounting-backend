package com.unionsg.xaccounting.entity;

import com.unionsg.xaccounting.entity.User.Role;
import com.unionsg.xaccounting.entity.User.User;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableBase {

        @Id
        @GeneratedValue
        private UUID id;
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(updatable = false, nullable = false)
//    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @CreatedBy
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @LastModifiedBy
    private User updatedBy;
}