package me.asu.fdf;

import me.asu.fdf.dao.FileIndexDao;
import me.asu.fdf.model.FileInfo;
import me.asu.fdf.util.GetOpt;
import me.asu.log.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

public class FindDupFiles {
    private static Map<String, String> description = new HashMap<String, String>() {{
        put("-h", "Print this message.");
        put("-t", "The table name.");
        put("-v", "Show verbose log.");
        put("-o", "The output file.");
    }};

    public static void main(String[] args) throws IOException {
        String options = "hvt:o:";
        GetOpt getOpt = new GetOpt(args, options);
        int c;
        List<String> tableNames = new ArrayList<>();
        boolean verbose = false;
        String outputFile = null;
        while ((c = getOpt.getNextOption()) != -1) {
            switch (c) {
                case 'h':
                    getOpt.printUsage("FindDupFiles", description);
                    System.exit(0);
                    break;
                case 't':
                    tableNames.add(getOpt.getOptionArg());
                    break;
                case 'v':
                    verbose = true;
                    break;
                case 'o':
                    outputFile = getOpt.getOptionArg();
                    break;
            }
        }

        if (verbose) {
            Log.set(Log.LEVEL_DEBUG);
        }

        find(outputFile, tableNames);
    }

    public static void find(String outputFile, List<String> tableNames) throws IOException {
        Objects.requireNonNull(outputFile);
        Objects.requireNonNull(tableNames);

        Map<String, List<FileInfo>> maps = new TreeMap<>();
        Path outputPath = Paths.get(outputFile);
        boolean exists = Files.exists(outputPath);
        if (!exists) {
            Files.createFile(outputPath);
        }
        FileIndexDao dao = new FileIndexDao();
        for (String tableName : tableNames) {
            Log.debug("FF","Loading files of " + tableName);
            List<FileInfo> fileInfos = dao.loadTable(tableName);
            for (FileInfo fi : fileInfos) {
                String key = fi.getMd5sum();
                maps.computeIfAbsent(key, k-> new ArrayList<>()).add(fi);
            }
        }
        Log.info(String.format("Get %d groups.", maps.size()));
        if (!maps.isEmpty()) {
            Iterator<Entry<String, List<FileInfo>>> iterator =
                    maps.entrySet().iterator();
            try (OutputStream outputStream = Files.newOutputStream(outputPath);
                 Writer writer = new OutputStreamWriter(outputStream)) {
                String lineSp = System.getProperty("line.separator", "\n");
                writer.write("# =================================================================");
                writer.write(lineSp);
                int counter = 0;
                while (iterator.hasNext()) {
                    Entry<String, List<FileInfo>> next = iterator.next();
                    String key = next.getKey();
                    List<FileInfo> value = next.getValue();
                    if (value.size() > 1) {
                        counter++;
                        String text = String.format("# %s%n", key);
                        writer.write(text);
                        for (FileInfo f : value) {
                            text = String.format("# rm -f \"%s\"%n", f.getFilePath());
                            writer.write(text);
                        }
                        writer.write("# -----------------------------------------------------------------");
                        writer.write(lineSp);
                    }
                }

                writer.write("# =================================================================");
                writer.write(lineSp);
                Log.info(String.format("Get %d duplicated groups%n", counter));
                Log.info(String.format("View %s to get detail.%n", outputFile));
            }
        } else {
            Log.warn("FF","No data.");
        }

        Log.info("FF","DONE.");
    }
}
