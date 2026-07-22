package com.unionsg.xaccounting.communication.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_log_id", nullable = false)
    private EmailLog emailLog;

    @Column(name = "file_id", nullable = false)
    private Long fileId;
}
