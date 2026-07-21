package com.unionsg.xaccounting.documenttemplate.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_template_designs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTemplateDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private DocumentTemplate template;

    @Column(name = "logo_file_id")
    private Long logoFileId;

    @Column(name = "logo_position", length = 20)
    private String logoPosition;

    @Column(name = "logo_width")
    private Integer logoWidth;

    @Column(name = "logo_height")
    private Integer logoHeight;

    @Column(name = "primary_color", length = 20)
    private String primaryColor;

    @Column(name = "secondary_color", length = 20)
    private String secondaryColor;

    @Column(name = "font_family", length = 100)
    private String fontFamily;

    @Column(name = "font_size")
    private Integer fontSize;
}

