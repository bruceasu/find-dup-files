package me.asu.fdf;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import me.asu.log.Log;

class ScanCmd {

    static void run(String[] args) throws Exception {
        String db = "cache.db";
        List<String> roots = new ArrayList<>();
        String sourceDisk = "local";
        int workers = Dedup.DEFAULT_WORKERS;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--db", "-d" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    db = args[++i];
                }
                case "--from-disk", "-f" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    sourceDisk = args[++i];
                }
                case "--workers", "-w" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    workers = Integer.parseInt(args[++i]);
                }
                case "--help", "-h" -> {
                    usage();
                    System.exit(0);
                }
                default -> roots.add(a);
            }
        }

        if (db == null || roots.isEmpty()) {
            usage();
            System.exit(1);
        }

        if (!Files.isRegularFile(Paths.get(db))) {
            Log.error("The database file does not exist: " + db);
            System.exit(1);
        }
        try (Connection c = DB.connect(db)) {
            DB.initDb(c);

            List<FileMeta> files = scanFiles(roots);
            Map<String, FileMeta> existing = DB.loadExisting(c);

            List<FileMeta> need = files.stream().filter(f -> {
                FileMeta e = existing.get(f.path());
                return e == null || e.size() != f.size() || e.mtime() != f.mtime();
            }).collect(Collectors.toList());

            Log.info("Total files: " + files.size());
            Log.info("Need quick-hash: " + need.size());
            final String src = sourceDisk;

            final int ioLimit = Math.max(1, workers);
            final Semaphore sem = new Semaphore(ioLimit);
            int batch = 0;
            try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletionService<FileFp> cs = new ExecutorCompletionService<>(pool);
               
                for (FileMeta f : need) {
                    cs.submit(() -> {
                        sem.acquire();
                        try {
                            return new FileFp(f.path(), f.size(), f.mtime(),
                                    Hash.quickHash(f.path(), f.size()), src, null);
                        } finally {
                            sem.release();
                        }
                    });
                }

                String sql = """
                        INSERT INTO file_fp
                        (path, size, mtime, quick_hash, source_disk, group_id,  updated_at)
                        VALUES (?, ?, ?,  ?, ?, ?, ?)
                        ON CONFLICT(path) DO UPDATE SET
                          size=excluded.size,
                          mtime=excluded.mtime,
                          quick_hash=excluded.quick_hash,
                          source_disk=excluded.source_disk,
                          group_id=excluded.group_id,
                          updated_at=excluded.updated_at
                        """;
                List<FileFp> batchList = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    for (int i = 0; i < need.size(); i++) {
                        FileFp fp = cs.take().get();
                        if (fp.quickHash() == null) continue;

                        batchList.add(fp);
                        if (batchList.size() < 500) continue;
                        
                        batch += DB.insertFileFp(ps, batchList.toArray(new FileFp[0]));
                        batchList.clear();
                        
                    }
                    if (!batchList.isEmpty()) {
                        batch += DB.insertFileFp(ps, batchList.toArray(new FileFp[0]));
                        batchList.clear();
                    }
                    c.commit();
                }
            }
            Log.info("Scanned files: " + files.size());
            Log.info("Updated fingerprints: " + batch);
        }
    }

    static void usage() {
        String usage = """
                Usage: java -jar fdf.jar scan --db <db> <root...> [--from-disk X] [--workers N]
                Options:
                    --db, -d <db>               SQLite database file
                    --from-disk, -f <path>      Base path on disk for scanning (default: local)
                    --workers, -w <N>           Number of worker threads (default: 3)
                Example:
                    java -jar fdf.jar scan --db dedup.db /path/to/scan1 /path/to/scan2 --from-disk X --workers 4
                """;
        System.err.println(usage);
    }

    static List<FileMeta> scanFiles(List<String> roots) throws IOException {
        List<FileMeta> out = new ArrayList<>();
        for (String r : roots) {
            Path root = Paths.get(r);
            if (!Files.exists(root))
                continue;
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                    if (a.isRegularFile() && a.size() > 0) {
                        out.add(new FileMeta(f.toAbsolutePath().toString(), a.size(),
                                a.lastModifiedTime().toMillis()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return out;
    }
}