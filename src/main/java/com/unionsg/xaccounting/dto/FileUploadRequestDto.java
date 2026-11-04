package com.unionsg.xaccounting.dto;

import com.unionsg.xaccounting.enums.EntityType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class FileUploadRequestDto {
    @NotNull
    private EntityType entityType;

    @NotNull
    private String entityId;

    private String description;

    /** Document ID to give the file, e.g. "DOC-3". Journal files get the next free one when omitted. */
    private String referenceNo;

    @NotNull
    private UUID uploadedBy;

    @Override
    public String toString(){
        return "FileUploadRequestDto{"+
                "entity-type='"+entityType.toString()+'\''+
                "entity-id='"+entityId.toString()+'\''+
                "description='"+description+'\''+
                '}';

    }
}
