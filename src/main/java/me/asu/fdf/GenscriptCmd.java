package me.asu.fdf;

import me.asu.log.Log;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class GenscriptCmd {
    public static void run(String[] args) throws Exception {
        if (args.length < 1)
            Dedup.usage();
        String db = "cache.db";
        String prefix = "delete_dup";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--db", "-d" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    db = args[++i];
                }
                case "--out-prefix", "-p" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    prefix = args[++i];
                }
                case "--help", "-h" -> usage();
                default-> db = args[i];
            }
        }

        if (!Files.isRegularFile(Paths.get(db))) {
            Log.error("The database file does not exist: " + db);
            System.exit(1);
        }

        try (Connection c = DB.connect(db);
                PrintWriter csv = new PrintWriter(prefix + ".csv");
                PrintWriter sh = new PrintWriter(prefix + ".sh");
                PrintWriter ps = new PrintWriter(prefix + ".ps1")) {

            csv.println("group_id,action,source_disk,path");

            String sql = """
                        SELECT group_id, path, source_disk
                        FROM file_fp
                        WHERE group_id IS NOT NULL
                        ORDER BY group_id;
                    """;

            DB.queryWithCallBack(c, sql, rs -> {
                try {
                    int current = -1;
                    boolean keep = false;
                    while (rs.next()) {
                        int gid = rs.getInt(1);
                        String path = rs.getString(2);
                        String disk = rs.getString(3);

                        if (gid != current) {
                            current = gid;
                            keep = true;
                            sh.println("\n# Group " + gid);
                            ps.println("\n# Group " + gid);
                        }

                        if (keep) {
                            sh.println("# keep: " + path);
                            ps.println("# keep: " + path);
                            csv.printf("%d,KEEP,%s,%s%n", gid, disk, path);
                            keep = false;
                        } else {
                            sh.println("# rm -f \"" + path + "\"");
                            ps.println("# Remove-Item \"" + path + "\"");
                            csv.printf("%d,DELETE,%s,%s%n", gid, disk, path);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    static void usage() {
        System.out.println("""
                prune        Prune missing files from DB
                options:
                    --db, -d <db>               SQLite database file
                    --from-disk, -f <path>      Base path on disk for scanning or verifying
                specific to gencript command:
                    --out-prefix, -p <prefix>   Output file prefix for delete scripts
                
                Usage:
                java -jar fdf.jar genscript --db <db> [--out-prefix delete_dup]   (default: delete_dup)
                """);
    }

}