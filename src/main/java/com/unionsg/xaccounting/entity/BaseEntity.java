package com.unionsg.xaccounting.entity;
import com.unionsg.xaccounting.security.auth.UserPrincipal;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

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

        String currentUserId = resolveCurrentUserId();

        if (this.createdBy == null) {
            this.createdBy = currentUserId;
        }

        if (this.updatedBy == null) {
            this.updatedBy = currentUserId;
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

    // =========================
    // Getters and Setters
    // =========================

//    public Long getId() {
//        return id;
//    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public LocalDateTime getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public String getCreatedBy() {
//        return createdBy;
//    }
//
//    public void setCreatedBy(String createdBy) {
//        this.createdBy = createdBy;
//    }
//
//    public String getUpdatedBy() {
//        return updatedBy;
//    }
//
//    public void setUpdatedBy(String updatedBy) {
//        this.updatedBy = updatedBy;
//    }
//
//    public Boolean getDeleted() {
//        return deleted;
//    }
//
//    public void setDeleted(Boolean deleted) {
//        this.deleted = deleted;
//    }
//
//    public LocalDateTime getDeletedAt() {
//        return deletedAt;
//    }
//
//    public String getDeletedBy() {
//        return deletedBy;
//    }
//
//    public Long getVersion() {
//        return version;
//    }
}