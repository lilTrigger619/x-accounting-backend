package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.entity.AuditableBase;
import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Turns leftover varchar {@code created_by} / {@code updated_by} columns into uuid foreign keys
 * to {@code users}, on every start, before any seeder runs.
 *
 * <p>Those columns used to be plain strings. They are now {@code @ManyToOne User} joins
 * ({@link BaseEntity#getCreatedBy()}, {@link AuditableBase}). {@code ddl-auto=update} tries to
 * change the type but Postgres refuses ("cannot be cast automatically to type uuid"), so a
 * database first built from the older code kept varchar columns. Hibernate then read the string
 * back into the User id and every save of a journal failed with
 * "Cannot cast java.lang.String to java.util.UUID". This is manual migration 001 done for you.</p>
 *
 * <p>Old values are kept where they can be: a value that is a user's id stays, a value that is a
 * user's email becomes that user's id, anything else becomes null (no creator recorded).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CreatedByColumnRepairRunner implements ApplicationRunner {

    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final EntityManagerFactory entityManagerFactory;

    @Override
    public void run(ApplicationArguments args) {
        for (String[] target : userColumns()) {
            String table = target[0];
            String column = target[1];
            try {
                if (isVarchar(table, column)) {
                    tx.executeWithoutResult(s -> repair(table, column));
                    log.info("Converted {}.{} from varchar to a uuid reference to users", table, column);
                }
            } catch (RuntimeException e) {
                log.error("Could not convert {}.{} to uuid; saves on this table will fail until it is fixed. {}",
                        table, column, e.getMessage());
            }
        }
    }

    /** Every (table, column) that the entity model maps as a join to User. */
    List<String[]> userColumns() {
        Set<String> seen = new TreeSet<>();
        List<String[]> out = new ArrayList<>();
        SessionFactoryImplementor sf = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        sf.getMappingMetamodel().forEachEntityDescriptor(descriptor -> {
            Class<?> type = descriptor.getMappedClass();
            if (!(descriptor instanceof AbstractEntityPersister persister)) {
                return;
            }
            String table = persister.getTableName();
            List<String> columns = new ArrayList<>();
            if (BaseEntity.class.isAssignableFrom(type)) {
                columns.add("created_by");
            } else if (AuditableBase.class.isAssignableFrom(type)) {
                columns.add("created_by");
                columns.add("updated_by");
            }
            for (String column : columns) {
                if (seen.add(table + "." + column)) {
                    out.add(new String[]{table, column});
                }
            }
        });
        return out;
    }

    private boolean isVarchar(String table, String column) {
        List<String> types = jdbc.queryForList("""
                SELECT data_type FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?
                """, String.class, table, column);
        return !types.isEmpty() && ("character varying".equals(types.get(0)) || "text".equals(types.get(0)));
    }

    private void repair(String table, String column) {
        String t = quote(table);
        String c = quote(column);
        // An email becomes that user's id.
        jdbc.update("UPDATE " + t + " x SET " + c + " = (SELECT u.id::text FROM users u WHERE lower(u.email) = lower(trim(x." + c + ")))"
                + " WHERE x." + c + " IS NOT NULL AND trim(x." + c + ") !~ '" + UUID_PATTERN + "'");
        // Anything that still isn't a known user's id can't be kept as a reference.
        jdbc.update("UPDATE " + t + " x SET " + c + " = NULL WHERE x." + c + " IS NOT NULL AND ("
                + "trim(x." + c + ") !~ '" + UUID_PATTERN + "'"
                + " OR NOT EXISTS (SELECT 1 FROM users u WHERE u.id::text = lower(trim(x." + c + "))))");
        jdbc.execute("ALTER TABLE " + t + " ALTER COLUMN " + c + " TYPE uuid USING lower(trim(" + c + "))::uuid");

        Integer fks = jdbc.queryForObject("""
                SELECT count(*) FROM pg_constraint con
                JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = ANY (con.conkey)
                WHERE con.contype = 'f' AND con.conrelid = to_regclass(?) AND a.attname = ?
                """, Integer.class, table, column);
        if (fks == null || fks == 0) {
            String name = quote(("fk_" + table + "_" + column).length() > 63
                    ? ("fk_" + table + "_" + column).substring(0, 63)
                    : "fk_" + table + "_" + column);
            jdbc.execute("ALTER TABLE " + t + " ADD CONSTRAINT " + name
                    + " FOREIGN KEY (" + c + ") REFERENCES users(id)");
        }
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
