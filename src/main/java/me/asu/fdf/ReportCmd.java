package me.asu.fdf;

import me.asu.log.Log;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReportCmd {

    public static void run(String[] args) throws Exception {
        if (args.length < 1)
            Dedup.usage();

        String db = "cache.db";
        String out = "report.csv";
        String mode = "quick";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--db", "-d" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    db = args[++i];
                }
                case "--out", "-o" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    out = args[++i];
                }
                case "--mode", "-m" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    mode = args[++i];
                }
                case "--help", "-h" -> {
                    usage();
                    System.exit(0);
                }
                default ->  db = args[i];
            }
        }

        if (!Files.isRegularFile(Paths.get(db))) {
            Log.error("The database file does not exist: " + db);
            System.exit(1);
        }

        if ("full".equalsIgnoreCase(mode)) {
            runReportFull(db, out);
        } else {
            runReportQuick(db, out);
        }
    }

    static void usage() {
        String usage = """
                Usage: java -jar fdf.jar report [options]
                Options:
                    --db, -d <db>           SQLite database file (default: cache.db)
                    --out, -o <file>        Output CSV file (default: report.csv)
                    --mode, -m <mode>       Report mode: [quick|full] (default: quick)
                """;
        System.err.println(usage);
    }

    static void runReportFull(String db, String out) throws Exception {
        try (Connection c = DB.connect(db); PrintWriter pw = new PrintWriter(out)) {

            pw.println("full_group_id,keep_candidate,source_disk,path,size,mtime,full_hash");

            String sql = """
                        SELECT full_hash
                        FROM file_fp
                        WHERE full_hash IS NOT NULL
                        GROUP BY full_hash
                        HAVING COUNT(*) >= 2;
                    """;

            int gid = 1;
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {

                while (rs.next()) {
                    String fh = rs.getString(1);

                    List<FileFpRowFull> rows = loadFullGroup(c, fh);

                    // Reuse the same keep-candidate sorting rule.
                    rows.sort(
                            Comparator.comparing((FileFpRowFull r) -> !"local".equals(r.sourceDisk))
                                    .thenComparing(r -> -r.mtime));

                    boolean first = true;
                    for (FileFpRowFull r : rows) {
                        pw.printf("%d,%s,%s,%s,%d,%d,%s%n", gid, first ? "YES" : "NO", r.sourceDisk,
                                r.path, r.size, r.mtime, fh);
                        first = false;
                    }
                    gid++;
                }
            }
        }
    }

    static List<FileFpRowFull> loadFullGroup(Connection c, String fullHash) throws SQLException {

        List<FileFpRowFull> l = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                    SELECT path,size,mtime,source_disk
                    FROM file_fp
                    WHERE full_hash=?;
                """)) {
            ps.setString(1, fullHash);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(new FileFpRowFull(rs.getString(1), rs.getLong(2), rs.getLong(3),
                            rs.getString(4)));
                }
            }
        }
        return l;
    }

    static void runReportQuick(String db, String out) throws Exception {
        try (Connection c = DB.connect(db); PrintWriter pw = new PrintWriter(out)) {

            pw.println("group_id,keep_candidate,source_disk,path,size,mtime");

            String sql = """
                        SELECT size, quick_hash
                        FROM file_fp
                        WHERE quick_hash IS NOT NULL
                        GROUP BY size, quick_hash
                        HAVING COUNT(*) >= 2;
                    """;

            DB.queryWithCallBack(c, sql, rs -> {
                int gid = 1;
                try {
                    while (rs.next()) {
                        long size = rs.getLong(1);
                        String qh = rs.getString(2);

                        List<FileFpRow> rows = loadGroup(c, size, qh);
                        rows.sort(Comparator.comparing((FileFpRow r) -> "local".equals(r.sourceDisk()) ? 0 : 1)
                                .thenComparing(r -> -r.mtime()));

                        boolean first = true;
                        for (FileFpRow r : rows) {
                            pw.printf("%d,%s,%s,%s,%d,%d%n", gid, first ? "YES" : "NO", r.sourceDisk(),
                                    r.path(), r.size(), r.mtime());
                            first = false;
                        }
                        updateGroupId(c, gid, rows);
                        gid++;
                    }
                    c.commit();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    static List<FileFpRow> loadGroup(Connection c, long size, String qh) throws SQLException {
        List<FileFpRow> l = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT path,size,mtime,source_disk FROM file_fp WHERE size=? AND quick_hash=?")) {
            ps.setLong(1, size);
            ps.setString(2, qh);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    l.add(new FileFpRow(rs.getString(1), rs.getLong(2), rs.getLong(3),
                            rs.getString(4)));
                }
            }
        }
        return l;
    }

    static void updateGroupId(Connection c, int gid, List<FileFpRow> rows) throws SQLException {
        try (PreparedStatement ps =
                c.prepareStatement("UPDATE file_fp SET group_id=? WHERE path=?")) {
            for (FileFpRow r : rows) {
                ps.setInt(1, gid);
                ps.setString(2, r.path());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    record FileFpRowFull(String path, long size, long mtime, String sourceDisk) {
    }

}