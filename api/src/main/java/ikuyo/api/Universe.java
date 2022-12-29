package ikuyo.api;

public record Universe(int index, int seed, boolean autoExpand) {
    //language=PostgreSQL
    public static final String createTableSql = """
        create table universe(
            index serial primary key,
            seed int not null default random(),
            auto_expand boolean not null
        );
        """;
}
