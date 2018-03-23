package co.cdjones.jooq.schema_update;

import org.jooq.*;
import org.jooq.tools.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.jooq.DDLFlag.*;
import static org.jooq.DDLFlag.FOREIGN_KEY;
import static org.jooq.impl.DSL.constraint;

public class JooqIncrementalSchemaUpdate {

    private final DSLContext ctx;
    private final EnumSet<DDLFlag> flags;

    public static List<Exception> updateSchema(DSLContext ctx, Schema schema) {
        List<Exception> exceptions = new ArrayList<>();

        JooqIncrementalSchemaUpdate updater = new JooqIncrementalSchemaUpdate(ctx);

        updater.queries(schema).queryStream().forEach(query -> {
            try {
                ctx.execute(query);
            } catch (Exception ex) {
                exceptions.add(ex);
            }
        });
        return exceptions;
    }

    public JooqIncrementalSchemaUpdate(DSLContext ctx) {
        this(ctx, DDLFlag.values());
    }

    public JooqIncrementalSchemaUpdate(DSLContext ctx, DDLFlag... flags) {
        this.ctx = ctx;
        this.flags = EnumSet.noneOf(DDLFlag.class);

        for (DDLFlag flag : flags) {
            this.flags.add(flag);
        }
    }

    public Queries queries(Schema schema) {
        List<Query> queries = new ArrayList<Query>();

        Optional<Schema> existingSchema = ctx.meta().getSchemas().stream()
            .filter(schema1 -> schema1.getName().equals(schema.getName()))
            .findFirst();

        if (flags.contains(SCHEMA) && !StringUtils.isBlank(schema.getName())) {
            queries.add(ctx.createSchemaIfNotExists(schema.getName()));
        }

        if (flags.contains(TABLE)) {
            for (Table<?> table : schema.getTables()) {

                Table<?> existingTable = existingSchema.isPresent() ? existingSchema.get().getTable(table.getName()) : null;
                Map<String, ? extends Key<?>> existingTableConstraintMap = existingTable != null ? getKeysForTable(existingTable) : new HashMap<>();
                Map<String, Index> existingIndexes = existingTable != null ? getIndexesForTable(existingTable) : new HashMap<>();

                List<Constraint> constraints = new ArrayList<>();

                if (flags.contains(PRIMARY_KEY)) {
                    for (UniqueKey<?> key : table.getKeys()) {
                        if (key.isPrimary()) {
                            boolean add = true;

                            if (existingTableConstraintMap.containsKey(key.getName())) {
                                add = false;
                            }

                            if (add) {
                                constraints.add(constraint(key.getName()).primaryKey(key.getFieldsArray()));
                            }
                        }
                    }
                }

                if (flags.contains(UNIQUE)) {
                    for (UniqueKey<?> key : table.getKeys()) {
                        if (!key.isPrimary()) {
                            boolean add = true;

                            if (existingTableConstraintMap.containsKey(key.getName()) || existingIndexes.containsKey(key.getName())) {
                                add = false;
                            }

                            if (add) {
                                constraints.add(constraint(key.getName()).unique(key.getFieldsArray()));
                            }
                        }
                    }
                }

                if (existingTable == null) {
                    queries.add( ctx.createTableIfNotExists(table).columns(table.fields()) );
                }

                if (constraints.size() > 0) {
                    constraints.forEach(constraint -> {
                        queries.add( ctx.alterTable(table).add(constraint) );
                    });
                }
            }

            if (flags.contains(FOREIGN_KEY)) {
                for (Table<?> table : schema.getTables()) {
                    Table<?> existingTable = existingSchema.isPresent() ? existingSchema.get().getTable(table.getName()) : null;
                    Map<String, ? extends Key<?>> keys = existingTable != null ? getReferencesForTable(existingTable) : new HashMap<>();

                    for (ForeignKey<?, ?> key : table.getReferences()) {
                        if (!keys.containsKey(key.getName())) {
                            queries.add(ctx.alterTable(table).add(constraint(key.getName()).foreignKey(key.getFieldsArray()).references(key.getKey().getTable(), key.getKey().getFieldsArray())));
                        }
                    }
                }
            }
        }

        return ctx.queries(queries);
    }

    public Queries queries(Catalog catalog) {
        List<Query> queries = new ArrayList<Query>();

        for (Schema schema : catalog.getSchemas())
            queries.addAll(Arrays.asList(queries(schema).queries()));

        return ctx.queries(queries);
    }

    private Map<String, ? extends Key<?>> getKeysForTable(Table<?> table) {
        if (table == null) return Collections.EMPTY_MAP;
        return table.getKeys().stream().collect(Collectors.toMap(UniqueKey::getName, it -> it));
    }

    private Map<String, Index> getIndexesForTable(Table<?> table) {
        if (table == null) return Collections.EMPTY_MAP;
        return table.getIndexes().stream().collect(Collectors.toMap(Index::getName, it -> it));
    }

    private Map<String, ? extends Key<?>> getReferencesForTable(Table<?> table) {
        if (table == null) return Collections.EMPTY_MAP;
        return table.getReferences().stream().collect(Collectors.toMap(Key::getName, it -> it));
    }
}
