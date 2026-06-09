package com.unionsg.xaccounting.MapperLayer;


import com.unionsg.xaccounting.dto.journal.JournalLineResponse;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.JournalSummaryResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
//import org.springframework.web.bind.annotation.Mapping;
//import org.mapstruct.Mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface JournalMapper {
    @Mapping(target = "lines", source = "lines")
    JournalResponse toResponse(JournalEntry entity);

    List<JournalResponse> toResponseList(List<JournalEntry> entities);

    JournalSummaryResponse toSummary(JournalEntry entity);

    List<JournalSummaryResponse> toSummaryList(
            List<JournalEntry> entities
    );

    @Mapping(target = "accountId", source = "account.id")
//    @Mapping(target = "accountCode", source = "account.code")
    @Mapping(target = "accountCode", source = "account.accountId")
//    @Mapping(target = "accountName", source = "account.name")
    @Mapping(target = "accountName", source = "account.accountName")
    JournalLineResponse toLineResponse(JournalLine line);

    List<JournalLineResponse> toLineResponseList(
            List<JournalLine> lines
    );
}
