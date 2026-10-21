package com.unionsg.xaccounting.repository.fixedasset;

import com.unionsg.xaccounting.entity.fixedasset.AssetTransaction;
import com.unionsg.xaccounting.enums.fixedasset.AssetTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssetTransactionRepository extends JpaRepository<AssetTransaction, Long> {

    List<AssetTransaction> findByAssetIdOrderByTransactionDateAscIdAsc(Long assetId);

    List<AssetTransaction> findByRunLineId(Long runLineId);

    /** Rows dated on or before {@code asOf}, for as-of balances. */
    @Query("""
            select t from AssetTransaction t join fetch t.asset a
            where t.transactionDate <= :asOf and a.deleted = false
            order by t.transactionDate asc, t.id asc
            """)
    List<AssetTransaction> findUpTo(@Param("asOf") LocalDate asOf);

    @Query("""
            select t from AssetTransaction t join fetch t.asset a
            where t.transactionDate between :from and :to and a.deleted = false
            order by t.transactionDate asc, t.id asc
            """)
    List<AssetTransaction> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** The newest row of the asset that is not itself a reversal and has not been reversed. */
    @Query("""
            select t from AssetTransaction t
            where t.asset.id = :assetId and t.type <> com.unionsg.xaccounting.enums.fixedasset.AssetTransactionType.REVERSAL
              and t.reversedAt is null
            order by t.transactionDate desc, t.id desc
            """)
    List<AssetTransaction> findLiveNewestFirst(@Param("assetId") Long assetId);

    boolean existsByAssetIdAndTypeInAndReversedAtIsNull(Long assetId, Collection<AssetTransactionType> types);

    Optional<AssetTransaction> findFirstByJournalId(Long journalId);

    /** Journals posted by this module, for tracing and cash-flow classification. */
    @Query("select distinct t.journal.id from AssetTransaction t where t.journal is not null")
    List<Long> findAllJournalIds();
}
