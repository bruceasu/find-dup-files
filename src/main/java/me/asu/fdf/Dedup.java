package me.asu.fdf;
import java.util.Arrays;

/*
 * Dedup CLI subcommands: scan - collect file fingerprints (quick hash only) report - group
 * duplicates and output CSV genscript - generate commented delete scripts (bash + powershell)
 */
public class Dedup {

    static final int CHUNK = 1024 * 1024; // 1MB
    static final int DEFAULT_WORKERS = 3;

    public static void main(String[] args) throws Exception {
        if (args.length < 1)
            usage();

        String cmd = args[0];
        switch (cmd) {
            case "scan" -> ScanCmd.run(Arrays.copyOfRange(args, 1, args.length));
            case "report" -> ReportCmd.run(Arrays.copyOfRange(args, 1, args.length));
            case "genscript" -> GenscriptCmd.run(Arrays.copyOfRange(args, 1, args.length));
            case "verify" -> VerifyCmd.run(Arrays.copyOfRange(args, 1, args.length));
            case "maintain" -> MaintainCmd.run(Arrays.copyOfRange(args, 1, args.length));
            case "prune" -> PruneCmd.run(Arrays.copyOfRange(args, 1, args.length));
            case "--help", "-h" -> usage();
            case "help" -> {
                if (args.length >= 2) {
                    String helpCmd = args[1];
                    switch (helpCmd) {
                        case "scan" -> ScanCmd.usage();
                        case "report" -> ReportCmd.usage();
                        case "genscript" -> GenscriptCmd.usage();
                        case "verify" -> VerifyCmd.usage();
                        case "maintain" -> MaintainCmd.usage();
                        case "prune" -> PruneCmd.usage();
                        default -> usage();
                    }
                } else {
                    usage();
                }
                System.exit(1);
            }
            default -> usage();
        }
    }

    static void usage() {
        System.err.println(
                """
                java -jar fdf.jar <command> [options]
                Commands:
                    scan         Scan files and collect fingerprints into DB
                    report       Generate duplicate report from DB
                    genscript    Generate delete scripts from DB
                    verify       Verify file hashes in DB
                    maintain     Maintain the database (SQLite PRAGMA operations)
                    prune        Prune missing files from DB
                options:
                    --db, -d <db>               SQLite database file
                    --from-disk, -f <path>      Base path on disk for scanning or verifying
                    --workers, -w <N>           Number of worker threads (default: 3)
                    --out, -o <file>            Output file
                    --group-id, -g <N>          Group ID for verify command
               specific to maintain command:
                    --mode, -m <mode>           Mode for maintain command: [quick|full]
                specific to gencript command:
                    --out-prefix, -p <prefix>   Output file prefix for delete scripts
                specific to maintain command:
                    --mode, -m <mode>           Mode for maintain command: [init|incremental|vacuum-into]
                    --pages, -p <N>             Number of pages for vacuum-into (default: 100) 
                specific to prune command:
                    --prefix <path>             Prefix path to prune (use '/' for all)
                    --dry-run                   Dry run mode, do not actually delete
                Examples:
                    java -jar fdf.jar scan --db <db> <root...> [--from-disk X] [--workers N]
                    java -jar fdf.jar report --db <db> [--out report.csv]   (default: report.csv)
                    java -jar fdf.jar genscript --db <db> [--out-prefix delete_dup]   (default: delete_dup)
                    java -jar fdf.jar verify --db <db> --group-id N --from-disk X [--workers N] 
                    java -jar fdf.jar maintain --db <db> --mode [init|incremental|vacuum-into] [--out file] [--pages N] 
                    java -jar fdf.jar prune --db <db> --from-disk local [--prefix PREFIX] [--dry-run]
                """);
        System.exit(1);
    }

}