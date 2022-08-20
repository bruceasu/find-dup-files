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

public class Difference {

    private static Map<String, String> description = new HashMap<String, String>() {{
        put("-h", "Print this message.");
        put("-a", "The left table name.");
        put("-b", "The right table name.");
        put("-v", "Show verbose log.");
        put("-o", "The output file.");
    }};

    public static void main(String[] args) throws IOException {
        String options = "hva:b:o:";
        GetOpt getOpt = new GetOpt(args, options);
        int c;
        String tableLeft = null, tableRight = null;
        boolean verbose = false;
        String outputFile = null;
        while ((c = getOpt.getNextOption()) != -1) {
            switch (c) {
                case 'h':
                    getOpt.printUsage("Difference", description);
                    System.exit(0);
                    break;
                case 'a':
                    tableLeft = getOpt.getOptionArg();
                    break;
                case 'b':
                    tableRight = getOpt.getOptionArg();
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

        diff(outputFile, tableLeft, tableRight);
    }


    private static void diff(String outputFile,
                             String tableLeft,
                             String tableRight) throws IOException {
        Objects.requireNonNull(outputFile);
        Objects.requireNonNull(tableLeft);
        Objects.requireNonNull(tableRight);
        Path outputPath = Paths.get(outputFile);
        boolean exists = Files.exists(outputPath);
        if (!exists) {
            Files.createFile(outputPath);
        }
        FileIndexDao dao = new FileIndexDao();
        List<FileInfo> left = dao.loadTable(tableLeft);
        List<FileInfo> right = dao.loadTable(tableRight);
        Map<String, List<FileInfo>> leftMap = new HashMap<>();
        Map<String, List<FileInfo>> rightMap = new HashMap<>();
        for (FileInfo fileInfo : left) {
            String key = fileInfo.getMd5sum();
            leftMap.putIfAbsent(key, new ArrayList<>());
            leftMap.get(key).add(fileInfo);
        }
        for (FileInfo fileInfo : right) {
            String key = fileInfo.getMd5sum();
            rightMap.putIfAbsent(key, new ArrayList<>());
            rightMap.get(key).add(fileInfo);
        }
        Set<String> leftKeys = new HashSet<>(leftMap.keySet());
        Set<String> rightKeys = new HashSet<>(rightMap.keySet());
        leftKeys.removeAll(rightKeys);

        try (OutputStream outputStream = Files.newOutputStream(outputPath);
             Writer writer = new OutputStreamWriter(outputStream)) {
            for (String key : leftKeys) {
                writer.write("# " + key + "\n");
                List<FileInfo> fileInfos = leftMap.get(key);
                for (FileInfo fi : fileInfos) {
                    writer.write('"');
                    writer.write(fi.getFilePath().replace("\"", "\\\""));
                    writer.write("\"\n");
                }
            }
        }
        Log.info("DONE.");
    }
}
