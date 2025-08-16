package me.asu.fdf;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import me.asu.log.Log;

public class VerifyCmd {
    static void run(String[] args) throws Exception {
        String db = "cache.db";
        Integer groupId = null;
        String fromDisk = null;
        int workers = 1;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--db", "-d" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    db = args[++i];
                }
                case "--group-id", "-g" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    groupId = Integer.parseInt(args[++i]);
                }
                case "--from-disk", "-f" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    fromDisk = args[++i];
                }
                case "--workers", "-w" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    workers = Integer.parseInt(args[++i]);
                }
                case "--help", "-h" -> usage();
                default -> db = args[i];
            }
        }

        if (db == null || groupId == null || fromDisk == null) {
            Log.error("verify requires --db, --group-id and --from-disk");
            usage();
            System.exit(1);
        }
        if (!Files.isRegularFile(Paths.get(db))) {
            Log.error("The database file does not exist: " + db);
            System.exit(1);
        }

        try (Connection c = DB.connect(db)) {
            DB.initDb(c);

            List<VerifyTarget> targets = loadVerifyTargets(c, groupId, fromDisk);

            if (targets.isEmpty()) {
                System.out.println("No files to verify (maybe already full-hashed).");
                return;
            }

            System.out.println("Verifying full hash:");
            System.out.println("  group_id   = " + groupId);
            System.out.println("  from_disk = " + fromDisk);
            System.out.println("  files     = " + targets.size());

            ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, workers));
            CompletionService<FullHashResult> cs = new ExecutorCompletionService<>(pool);

            for (VerifyTarget t : targets) {
                cs.submit(() -> new FullHashResult(t.path, fullFileHash(t.path)));
            }

            String sql = """
                        UPDATE file_fp
                        SET full_hash=?, updated_at=?
                        WHERE path=?;
                    """;

            long now = Instant.now().toEpochMilli();
            int done = 0;

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < targets.size(); i++) {
                    FullHashResult r = cs.take().get();
                    if (r.fullHash == null)
                        continue;

                    ps.setString(1, r.fullHash);
                    ps.setLong(2, now);
                    ps.setString(3, r.path);
                    ps.addBatch();

                    if (++done % 50 == 0) {
                        ps.executeBatch();
                        c.commit();
                        System.out.println("  verified: " + done);
                    }
                }
                ps.executeBatch();
                c.commit();
            } finally {
                pool.shutdownNow();
            }

            System.out.println("Verify finished. Total verified: " + done);
        }
    }

    static void usage() {
        String usage = """
                verify --db <db> --group-id <N> --from-disk <path> [--workers N]
                Options:
                    --db, -d <db>               SQLite database file
                    --group-id, -g <N>          Group ID for verify command
                    --from-disk, -f <path>      Base path on disk for verifying
                    --workers, -w <N>           Number of worker threads (default: 1)
                """;
        System.err.println(usage);
    }

    static List<VerifyTarget> loadVerifyTargets(Connection c, int groupId, String fromDisk)
            throws SQLException {

        List<VerifyTarget> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                    SELECT path
                    FROM file_fp
                    WHERE group_id=?
                    AND source_disk=?
                    AND full_hash IS NULL;
                """)) {
            ps.setInt(1, groupId);
            ps.setString(2, fromDisk);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new VerifyTarget(rs.getString(1)));
                }
            }
        }
        return list;
    }

    static String fullFileHash(String path) {
        try (FileChannel ch = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteBuffer buf = ByteBuffer.allocateDirect(4 * 1024 * 1024); // 4MB buffer

            while (true) {
                buf.clear();
                int n = ch.read(buf);
                if (n <= 0)
                    break;
                buf.flip();

                byte[] tmp = new byte[n];
                buf.get(tmp);
                md.update(tmp);
            }
            return Hash.hex(md.digest());
        } catch (Exception e) {
            System.err.println("Full hash failed: " + path + " : " + e.getMessage());
            return null;
        }
    }

    record VerifyTarget(String path) {
    }

    record FullHashResult(String path, String fullHash) {
    }
}