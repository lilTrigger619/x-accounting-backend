package com.unionsg.xaccounting.communication.domain;

import com.unionsg.xaccounting.communication.enums.EmailProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", length = 100)
    private String companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailProvider provider;

    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(length = 255)
    private String username;

    @Column(name = "encrypted_password", length = 512)
    private String encryptedPassword;

    @Column(name = "from_email", length = 255)
    private String fromEmail;

    @Column(name = "from_name", length = 255)
    private String fromName;

    @Column(name = "use_tls")
    private boolean useTls = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
