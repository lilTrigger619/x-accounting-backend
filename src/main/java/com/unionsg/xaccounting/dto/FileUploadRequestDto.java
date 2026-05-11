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
