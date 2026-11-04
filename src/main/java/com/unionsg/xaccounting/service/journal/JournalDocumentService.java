package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.entity.FileEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.repository.FileRepository;
import com.unionsg.xaccounting.repository.journal.JournalLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Document IDs on journal attachments ("DOC-1", "DOC-2", ...) and the journal lines that quote
 * them, the way older ledgers let a line point at a voucher in the batch.
 */
@Service
@RequiredArgsConstructor
public class JournalDocumentService {

    static final String PREFIX = "DOC-";
    private static final Pattern NUMBERED = Pattern.compile("^DOC-(\\d+)$");

    private final FileRepository fileRepository;
    private final JournalLineRepository journalLineRepository;

    /** Trimmed and upper-cased; blank becomes null. */
    public static String normalize(String ref) {
        if (ref == null) {
            return null;
        }
        String trimmed = ref.trim().toUpperCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** True for the attachment entity types journals use (GENERAL_JOURNAL, SALES_JOURNAL, ...). */
    public static boolean isJournalEntity(EntityType type) {
        return type != null && type.name().endsWith("_JOURNAL");
    }

    public static EntityType entityTypeOf(JournalEntry journal) {
        if (journal.getJournalType() == null) {
            return null;
        }
        try {
            return EntityType.valueOf(journal.getJournalType().name() + "_JOURNAL");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The journal's attachments by document ID. */
    public Map<String, FileEntity> documentsOf(JournalEntry journal) {
        EntityType type = entityTypeOf(journal);
        Map<String, FileEntity> byRef = new HashMap<>();
        if (type == null || journal.getId() == null) {
            return byRef;
        }
        for (FileEntity f : fileRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(type, String.valueOf(journal.getId()))) {
            String ref = normalize(f.getReferenceNo());
            if (ref != null) {
                byRef.putIfAbsent(ref, f);
            }
        }
        return byRef;
    }

    /** Refuses to post a journal whose lines quote a document that isn't attached to it. */
    public void assertReferencedDocumentsAttached(JournalEntry journal) {
        List<JournalLine> quoting = journal.getLines().stream()
                .filter(l -> normalize(l.getDocumentRef()) != null)
                .toList();
        if (quoting.isEmpty()) {
            return;
        }
        Map<String, FileEntity> docs = documentsOf(journal);
        for (JournalLine line : quoting) {
            String ref = normalize(line.getDocumentRef());
            if (!docs.containsKey(ref)) {
                throw new BadRequestException("Line " + line.getLineNumber() + " points at document " + ref
                        + ", which isn't attached to this journal. Attach it or clear the document on the line.");
            }
        }
    }

    /** The document ID for a new attachment: the one asked for if it's free, otherwise the next DOC-n. */
    public String assignReference(EntityType type, String entityId, String requested, List<String> takenInThisUpload) {
        Map<String, Boolean> taken = new HashMap<>();
        int max = 0;
        for (FileEntity f : fileRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(type, entityId)) {
            String ref = normalize(f.getReferenceNo());
            if (ref != null) {
                taken.put(ref, true);
                max = Math.max(max, number(ref));
            }
        }
        for (String ref : takenInThisUpload) {
            taken.put(ref, true);
            max = Math.max(max, number(ref));
        }
        String wanted = normalize(requested);
        if (wanted != null) {
            if (wanted.length() > 20) {
                throw new BadRequestException("A document ID can be at most 20 characters");
            }
            if (taken.containsKey(wanted)) {
                throw new BadRequestException("Document ID " + wanted + " is already used on this journal");
            }
            return wanted;
        }
        return PREFIX + (max + 1);
    }

    /** Refuses to delete an attachment that a line of its journal still points at. */
    public void assertNotReferenced(FileEntity file) {
        String ref = normalize(file.getReferenceNo());
        if (ref == null || !isJournalEntity(file.getEntityType()) || file.getEntityId() == null) {
            return;
        }
        long journalId;
        try {
            journalId = Long.parseLong(file.getEntityId());
        } catch (NumberFormatException e) {
            return;
        }
        if (journalLineRepository.existsByJournalEntryIdAndDocumentRefIgnoreCase(journalId, ref)) {
            throw new BadRequestException("A journal line points at " + ref
                    + ". Clear the document on that line before removing the file.");
        }
    }

    private static int number(String ref) {
        Matcher m = NUMBERED.matcher(ref);
        return m.matches() ? Integer.parseInt(m.group(1)) : 0;
    }
}
