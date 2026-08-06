package com.unionsg.xaccounting.documenttemplate.domain;

import com.unionsg.xaccounting.documenttemplate.enums.EmailType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_template_emails", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"template_id", "email_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTemplateEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private DocumentTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 20)
    private EmailType emailType;

    @Column(length = 500)
    private String subject;

    @Builder.Default
    @Column(name = "use_greeting", nullable = false)
    private boolean useGreeting = true;

    @Column(length = 100)
    private String salutation;

    @Column(name = "name_format", length = 50)
    private String nameFormat;

    @Column(columnDefinition = "TEXT")
    private String body;
}

