package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.entity.FileEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.repository.FileRepository;
import com.unionsg.xaccounting.repository.journal.JournalLineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalDocumentServiceTest {

    @Mock private FileRepository fileRepository;
    @Mock private JournalLineRepository journalLineRepository;

    @InjectMocks
    private JournalDocumentService service;

    private static FileEntity file(String ref) {
        FileEntity f = new FileEntity();
        f.setId("f-" + ref);
        f.setReferenceNo(ref);
        f.setOriginalName(ref + ".pdf");
        f.setEntityType(EntityType.GENERAL_JOURNAL);
        f.setEntityId("7");
        f.setIsDeleted(false);
        return f;
    }

    private static JournalEntry journalWithRefs(String... refs) {
        JournalEntry j = new JournalEntry();
        j.setId(7L);
        j.setJournalType(JournalType.GENERAL);
        int n = 1;
        for (String ref : refs) {
            JournalLine l = new JournalLine();
            l.setLineNumber(n++);
            l.setDocumentRef(ref);
            j.addLine(l);
        }
        return j;
    }

    @Test
    void firstAttachmentIsDoc1AndNumbersContinueAfterTheHighest() {
        when(fileRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(EntityType.GENERAL_JOURNAL, "7"))
                .thenReturn(List.of());
        assertThat(service.assignReference(EntityType.GENERAL_JOURNAL, "7", null, List.of())).isEqualTo("DOC-1");

        when(fileRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(EntityType.GENERAL_JOURNAL, "7"))
                .thenReturn(List.of(file("DOC-1"), file("DOC-4"), file("INV-22")));
        assertThat(service.assignReference(EntityType.GENERAL_JOURNAL, "7", null, List.of("DOC-5"))).isEqualTo("DOC-6");
    }

    @Test
    void keepsTheRequestedIdUnlessItIsTaken() {
        when(fileRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(EntityType.GENERAL_JOURNAL, "7"))
                .thenReturn(List.of(file("DOC-1")));
        assertThat(service.assignReference(EntityType.GENERAL_JOURNAL, "7", " doc-3 ", List.of())).isEqualTo("DOC-3");
        assertThatThrownBy(() -> service.assignReference(EntityType.GENERAL_JOURNAL, "7", "doc-1", List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DOC-1");
    }

    @Test
    void postingNeedsEveryQuotedDocumentToBeAttached() {
        when(fileRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(EntityType.GENERAL_JOURNAL, "7"))
                .thenReturn(List.of(file("DOC-1")));
        assertThatCode(() -> service.assertReferencedDocumentsAttached(journalWithRefs("DOC-1", null)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertReferencedDocumentsAttached(journalWithRefs("DOC-1", "DOC-2")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Line 2")
                .hasMessageContaining("DOC-2");
    }

    @Test
    void aQuotedAttachmentCannotBeDeleted() {
        when(journalLineRepository.existsByJournalEntryIdAndDocumentRefIgnoreCase(7L, "DOC-1")).thenReturn(true);
        assertThatThrownBy(() -> service.assertNotReferenced(file("DOC-1"))).isInstanceOf(BadRequestException.class);
        assertThatCode(() -> service.assertNotReferenced(file("DOC-2"))).doesNotThrowAnyException();
    }
}
