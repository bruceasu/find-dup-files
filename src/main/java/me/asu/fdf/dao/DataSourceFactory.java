package me.asu.fdf.dao;


import me.asu.log.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DataSourceFactory {

    public static Connection getConnection() throws SQLException {
        String driver = "org.h2.Driver";
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            // ignore
        }
        String path = Paths.get(System.getProperty("user.home"), ".local", "share","fdf.file-index").toString();
        String jdbcUrl = "jdbc:h2:" + path + ";TRACE_LEVEL_FILE=0";
        String username = "";
        String password = "";
        final Connection connection = DriverManager.getConnection(jdbcUrl, username, password);

        return connection;

    }
    // ANALYZE;SHUTDOWN COMPACT;

    public static List<String> listTables() {
        String sql = "select table_name from INFORMATION_SCHEMA.tables where TABLE_TYPE= 'BASE TABLE' and TABLE_SCHEMA='PUBLIC'";
        List<String> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(sql);
            rs = statement.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        } catch (SQLException t) {
            Log.warn("DB", t.getMessage(), t);
        } finally {
            closeQuietly(connection);
            closeQuietly(rs);
            closeQuietly(statement);
        }

        return list;
    }

    public static void compact() {
        Connection connection = null;
        Statement st = null;
        try {
            connection = DataSourceFactory.getConnection();
            st = connection.createStatement();
            st.execute("ANALYZE");          // 先更新统计信息（不缩小文件）
            st.execute("SHUTDOWN COMPACT"); // 再压缩并关闭数据库
            // 到这里连接已被关闭
        } catch (SQLException e) {
            Log.error("DB", e.getMessage(), e);
        } finally {
            closeQuietly(st);
            closeQuietly(connection);
        }

    }

    public static void drop(String tableName) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DataSourceFactory.getConnection();
            String sql = "drop table " + tableName;
            Log.debug("SQL: " + sql);
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            Log.error("DB", e.getMessage(), e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public static void createTable(String tableName) {
        //classpath:databases.sql => String
        StringBuilder sb = new StringBuilder();
        try (InputStream in = DataSourceFactory.class.getClassLoader()
                .getResourceAsStream("database.sql");

        ) {
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                throw new RuntimeException("database.sql script can't load please check it.");
            }
        } catch (Exception e) {
            Log.error("DB", e.getMessage(), e);
        }

        String sql = sb.toString().replace("$table_name", tableName);

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public static void closeQuietly(AutoCloseable a) {
        if (a != null) {
            return;
        }
        try {
            a.close();
        } catch (Exception e) {
            //ignored
        }
    }

}

