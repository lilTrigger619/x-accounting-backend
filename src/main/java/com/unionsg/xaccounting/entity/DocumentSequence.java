package com.unionsg.xaccounting.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "document_sequences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_sequence_code",
                        columnNames = "code"
                )
        }
)
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "prefix", length = 20)
    private String prefix;

    @Column(name = "current_value", nullable = false)
    private Long currentValue = 0L;

    @Column(name = "padding", nullable = false)
    private Integer padding = 5;

}
