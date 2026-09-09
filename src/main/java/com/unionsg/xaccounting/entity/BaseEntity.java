package com.unionsg.xaccounting.entity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.security.auth.UserPrincipal;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Version
    @Column(name = "version")
    private Long version;

    // =========================
    // Lifecycle Hooks
    // =========================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        // createdBy itself is populated automatically by AuditingEntityListener (@CreatedBy)
        // via the AuditorAware<User> bean; only updatedBy needs a manual default here.
        if (this.updatedBy == null) {
            this.updatedBy = resolveCurrentUserId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        String currentUserId = resolveCurrentUserId();

        if (currentUserId != null) {
            this.updatedBy = currentUserId;
        }
    }

    private String resolveCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUser().getId().toString();
        }

        return null;
    }

    // =========================
    // Soft Delete
    // =========================

    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}
