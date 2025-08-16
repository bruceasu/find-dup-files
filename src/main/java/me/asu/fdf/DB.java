package me.asu.fdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import me.asu.log.Log;

/**
 * ========================= Database =========================
 */
public class DB {
    // SQLite 压缩数据库文件的方法：
    // 一、最标准、最推荐：
    // sqlite3 cache.db "VACUUM;"
    // 二、空间不足时：VACUUM INTO（更安全）
    // sqlite3 cache.db "VACUUM INTO 'cache_compact.db';"
    // mv cache_compact.db cache.db
    // 三、轻量整理（不一定缩文件）：PRAGMA optimize;
    // sqlite3 cache.db "PRAGMA optimize;"
    // 四、减少“以后再膨胀”的配置（强烈建议）
    // 这是你这种长期跑 scan / report / verify 的工具必须做的。
    // 1️⃣ 启用 WAL + auto_vacuum
    // PRAGMA journal_mode = WAL;
    // PRAGMA auto_vacuum = INCREMENTAL;
    // ⚠️ auto_vacuum 只在建库前生效
    // 如果你已经有库，需要：
    // sqlite3 cache.db "PRAGMA auto_vacuum=INCREMENTAL; VACUUM;"
    // （这一步会重写一次）
    // 2️⃣ 以后“渐进回收”而不是一次性 VACUUM
    // sqlite3 cache.db "PRAGMA incremental_vacuum(200);"
    // 五、针对你 Dedup 工具的“最佳实践方案”
    // sqlite3 cache.db "VACUUM INTO 'cache_new.db';"
    // 第一次（或数据库明显变大时）
    // mv cache_new.db cache.db
    // 并初始化：
    // sqlite3 cache.db "
    //PRAGMA journal_mode=WAL;
    //PRAGMA auto_vacuum=INCREMENTAL;
    //"
    // 六、如何判断“需不需要 VACUUM”
    // sqlite3 cache.db "PRAGMA freelist_count;"
    // - 返回值很大（几千 / 几万页）
    // - 且 DB 文件明显大于预期
    // - 👉 该 VACUUM 了
    // 七、给你一个“最小记忆版总结”
    // > SQLite 压缩 = VACUUM
    // - 想一次压到最小：VACUUM
    // - 想安全一点：VACUUM INTO
    // - 想长期稳定：auto_vacuum=INCREMENTAL + incremental_vacuum


    public static String getDefaultDbPath() {
        Path p = Paths.get(System.getProperty("user.home"), ".local", "share", "fdf.file-index");
        Path parent = p.getParent();
        if (!Files.isDirectory(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                Log.error("Failed to create directory: " + parent);
            }
        }
        return p.toString();
    }

    public static Connection connect(String dbPath) throws SQLException {
        if (dbPath == null || dbPath.isEmpty()) {
            throw new IllegalArgumentException("Database path is null or empty");
        }
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        c.setAutoCommit(false);
        return c;
    }

    public static void createTable(Connection connection) 
    throws IOException, SQLException {
        // classpath:databases.sql => String
        StringBuilder sb = new StringBuilder();
        ClassLoader cl = DB.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream("database.sql")) {

            if (in == null) throw new RuntimeException("database.sql script can't load please check it.");

            try (Reader r = new InputStreamReader(in, "UTF-8"); 
                    BufferedReader reader = new BufferedReader(r)) {
                String line = null;
                while ((line = reader.readLine()) != null)
                    sb.append(line).append("\n"); 
            }
      
        }
              
        String sql = sb.toString();

        try (PreparedStatement statement = connection.prepareStatement(sql);) {
            statement.executeUpdate();
        }
    }
    
    public static void initDb(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS file_fp (
                        path TEXT PRIMARY KEY,
                        size INTEGER NOT NULL,
                        mtime INTEGER NOT NULL,
                        quick_hash TEXT,
                        full_hash TEXT,
                        source_disk TEXT,
                        group_id INTEGER,
                        updated_at INTEGER NOT NULL
                        );
                    """);
            st.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_size_qh ON file_fp(size, quick_hash);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_group ON file_fp(group_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_fullhash ON file_fp(full_hash);");
        }
        c.commit();
    }


    public static int insertFileFp(PreparedStatement ps, FileFp... fps) throws SQLException {
        for (FileFp fp : fps) {
            ps.setString(1, fp.path());
            ps.setLong(2, fp.size());
            ps.setLong(3, fp.mtime());
            ps.setString(4, fp.quickHash());
            ps.setString(5, fp.sourceDisk());
            if (fp.groupId() != null) {
                ps.setLong(6, fp.groupId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            ps.setLong(7, Instant.now().getEpochSecond());

            ps.addBatch();
        }

        int length = ps.executeBatch().length;
        ps.getConnection().commit();
        return length;

    }

    public static Map<String, FileMeta> loadExisting(Connection c) throws SQLException {
        return query(c, "SELECT path,size,mtime FROM file_fp", rs -> {
            Map<String, FileMeta> m = new HashMap<>();
            while (rs.next()) {
                m.put(rs.getString(1), new FileMeta(rs.getString(1), rs.getLong(2), rs.getLong(3)));
            }
            return m;
        });

    }

    public static long queryWithCallBack(Connection c, String sql, Consumer<ResultSet> consumer)
            throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            consumer.accept(rs);
            return rs.getRow();
        }
    }

    public static <T> T query(Connection c, String sql, ResultSetMapper<T> mapper)
            throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return mapper.map(rs);
        }
    }

    public static <T> Optional<T> queryOne(Connection c, String sql, ResultSetMapper<T> mapper)
            throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return Optional.of(mapper.map(rs));
            } else {
                return Optional.empty();
            }
        }
    }

    public static <T> List<T> queryList(Connection c, String sql, ResultSetMapper<T> mapper)
            throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<T> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapper.map(rs));
            }
            return out;
        }
    }
}
