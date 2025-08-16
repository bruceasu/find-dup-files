package me.asu.fdf;

import me.asu.log.Log;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

public class MaintainCmd {
    static void run(String[] args) throws Exception {
        if (args.length < 1)
            Dedup.usage();

        String db = "cache.db";
        String mode = null;
        String out = null;
        int pages = 100;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--db", "-d" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    db = args[++i];
                }
                case "--mode", "-m" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    mode = args[++i];
                }
                case "--out", "-o" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    out = args[++i];
                }
                case "--pages", "-p" -> {
                    if (i + 1 >= args.length) Dedup.usage();
                    pages = Integer.parseInt(args[++i]);
                }
                case "--help", "-h" -> {
                    usage();
                    System.exit(0);
                }
                default -> db = args[i];
            }
        }
        if (mode == null) {
            usage();
            System.exit(1);
        }
        if (!Files.isRegularFile(Paths.get(db))) {
            Log.error("The database file does not exist: " + db);
            System.exit(1);
        }

        try (Connection c = DB.connect(db); Statement st = c.createStatement()) {

            switch (mode) {

                case "init" -> {
                    System.out.println("Initializing DB: WAL + auto_vacuum=INCREMENTAL");
                    st.execute("PRAGMA journal_mode=WAL;");
                    st.execute("PRAGMA auto_vacuum=INCREMENTAL;");
                    st.execute("VACUUM;");
                    c.commit();
                    System.out.println("Init done.");
                }

                case "incremental" -> {
                    System.out.println("Incremental vacuum, pages=" + pages);
                    st.execute("PRAGMA incremental_vacuum(" + pages + ");");
                    c.commit();
                    System.out.println("Incremental vacuum done.");
                }

                case "vacuum-into" -> {
                    if (out == null) {
                        System.err.println("--out is required for vacuum-into");
                        System.exit(1);
                    }
                    System.out.println("VACUUM INTO " + out);
                    st.execute("VACUUM INTO '" + out + "';");
                    c.commit();
                    System.out.println("Vacuum into done.");
                }

                default -> Dedup.usage();
            }
        }
    }

    static void usage() {
        String usage = """
                maintain command:
                    --db, -d <db>               SQLite database file
                    --mode, -m <mode>           Mode for maintain command: [init|incremental|vacuum-into]
                    --out, -o <file>            Output file (required for vacuum-into)
                    --pages, -p <N>             Number of pages for vacuum-into (default: 100)
                Examples:
                    java -jar fdf.jar maintain --db <db> --mode [init|incremental|vacuum-into] [--out file] [--pages N]
                """;
        System.err.println(usage);
    }

}