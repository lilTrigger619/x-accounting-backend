package com.unionsg.xaccounting.documenttemplate.domain;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.documenttemplate.enums.TemplateStatus;
import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "document_type"}),
        @UniqueConstraint(columnNames = {"template_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate extends BaseEntity {

    @Column(name = "template_code", nullable = false, unique = true, length = 50)
    private String templateCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentLayout layout;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateStatus status;

    @OneToOne(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DocumentTemplateDesign design;

    @OneToOne(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DocumentTemplateContent content;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DocumentTemplateEmail> emails = new ArrayList<>();

    public void setDesign(DocumentTemplateDesign design) {
        this.design = design;
        if (design != null) {
            design.setTemplate(this);
        }
    }

    public void setContent(DocumentTemplateContent content) {
        this.content = content;
        if (content != null) {
            content.setTemplate(this);
        }
    }

    public void addEmail(DocumentTemplateEmail email) {
        emails.add(email);
        email.setTemplate(this);
    }

    public void removeEmail(DocumentTemplateEmail email) {
        emails.remove(email);
        email.setTemplate(null);
    }
}

