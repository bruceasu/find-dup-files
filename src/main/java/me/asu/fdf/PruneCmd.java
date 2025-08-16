package me.asu.fdf;

import me.asu.log.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/*
 * 在 prune 前后跑一次轻量维护 java Dedup maintain cache.db --mode incremental --pages 200
 *
 *
 * 原因：
 *
 * prune 本质是大量 DELETE
 *
 * 即便 auto_vacuum=INCREMENTAL，也需要触发回收
 */
public class PruneCmd {
    static void run(String[] args) throws Exception {
        String db = "cache.db";
        String fromDisk = "local";
        String prefix = null;
        boolean dryRun = false;

        // internal tuning
        final int DELETE_BATCH = 300;
        final int VACUUM_PAGES = 200;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
            case "--db", "-d" -> {
                if (i + 1 >= args.length) Dedup.usage();
                db = args[++i];
            }
            case "--from-disk" -> {
                if (i + 1 >= args.length) Dedup.usage();
                fromDisk = args[++i];
            }
            case "--prefix" -> {
                if (i + 1 >= args.length) Dedup.usage();
                prefix = args[++i];
            }
            case "--dry-run" -> {
                if (i + 1 >= args.length) Dedup.usage();
                dryRun = true;
            }
            case "--help", "-h" -> {
                usage();
                System.exit(0);
            }
            default -> db = args[i]; // as db
            }
        }

        if (prefix == null) {
            System.err.println("prune requires --prefix (use '/' for all)");
            System.exit(1);
        }
        if (!Files.isRegularFile(Paths.get(db))) {
            Log.error("The database file does not exist: " + db);
            System.exit(1);
        }

        String likePattern = prefix.equals("/") ? "%" : prefix + "%";

        System.out.println("Prune missing files (batched + auto vacuum):");
        System.out.println("  db        = " + db);
        System.out.println("  fromDisk  = " + fromDisk);
        System.out.println("  prefix    = " + prefix);
        System.out.println("  dryRun    = " + dryRun);

        int scanned = 0;
        int missing = 0;
        int deleted = 0;

        try (Connection c = DB.connect(db)) {

            c.setAutoCommit(false);

            /* ---------- pre-vacuum ---------- */
            if (!dryRun) {
                System.out.println("[pre] incremental_vacuum(" + VACUUM_PAGES + ")");
                try (Statement st = c.createStatement()) {
                    st.execute("PRAGMA incremental_vacuum(" + VACUUM_PAGES + ");");
                }
                c.commit();
            }

            try (PreparedStatement select = c.prepareStatement("""
                            SELECT path
                            FROM file_fp
                            WHERE source_disk = ? AND path LIKE ?
                            """,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
                 PreparedStatement delete = c.prepareStatement(
                         "DELETE FROM file_fp WHERE path = ?")) {

                select.setString(1, fromDisk);
                select.setString(2, likePattern);

                List<String> deleteBatch = new ArrayList<>(DELETE_BATCH);

                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        String path = rs.getString(1);
                        scanned++;

                        boolean exists;
                        try {
                            exists = Files.exists(Path.of(path));
                        } catch (Exception e) {
                            exists = false;
                        }

                        if (!exists) {
                            missing++;
                            deleteBatch.add(path);

                            if (dryRun) {
                                System.out.println("[DRY-RUN] missing: " + path);
                            }

                            if (!dryRun && deleteBatch.size() >= DELETE_BATCH) {
                                for (String p : deleteBatch) {
                                    delete.setString(1, p);
                                    delete.addBatch();
                                }
                                delete.executeBatch();
                                c.commit();

                                deleted += deleteBatch.size();
                                deleteBatch.clear();

                                System.out.println("  scanned=" + scanned
                                        + " missing=" + missing
                                        + " deleted=" + deleted);
                            }
                        }

                        if (scanned % 5000 == 0) {
                            System.out.println("  scanned=" + scanned
                                    + " missing=" + missing
                                    + " deleted=" + deleted);
                        }
                    }
                }

                // flush remaining deletes
                if (!dryRun && !deleteBatch.isEmpty()) {
                    for (String p : deleteBatch) {
                        delete.setString(1, p);
                        delete.addBatch();
                    }
                    delete.executeBatch();
                    c.commit();

                    deleted += deleteBatch.size();
                    deleteBatch.clear();
                }
            }

            /* ---------- post-vacuum ---------- */
            if (!dryRun) {
                System.out.println("[post] incremental_vacuum(" + VACUUM_PAGES + ")");
                try (Statement st = c.createStatement()) {
                    st.execute("PRAGMA incremental_vacuum(" + VACUUM_PAGES + ");");
                }
                c.commit();
            }

            System.out.println("Prune finished:");
            System.out.println("  scanned = " + scanned);
            System.out.println("  missing = " + missing);
            System.out.println("  deleted = " + deleted);
            System.out.println("  dryRun  = " + dryRun);
        }
    }

    static void usage() {
        String usage = """
                java -jar fdf.jar prune --db <db> --from-disk local [--prefix PREFIX] [--dry-run]
                Options:
                    --db, -d <db>               SQLite database file
                    --from-disk, -f <path>      Base path on disk for pruning (default: local)
                    --prefix <path>             Prefix path to prune (use '/' for all)
                    --dry-run                   Dry run mode, do not actually delete
                """;
        System.err.println(usage);
    }


}