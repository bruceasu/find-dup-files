package me.asu.fdf;

@FunctionalInterface
public interface ResultSetMapper<T> {
    T map(java.sql.ResultSet rs) throws java.sql.SQLException;
}
