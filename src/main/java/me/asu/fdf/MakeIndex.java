package me.asu.fdf;

import me.asu.fdf.dao.DataSourceFactory;
import me.asu.fdf.dao.FileIndexDao;
import me.asu.fdf.model.FileInfo;
import me.asu.fdf.util.GetOpt;
import me.asu.fdf.util.csv.CsvReader;
import me.asu.fdf.util.csv.CsvWriter;
import me.asu.fdf.util.csv.CsvWriter.Letters;
import me.asu.log.Log;
import org.h2.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MakeIndex {
    private static Map<String, String> description = new HashMap<String, String>() {{
        put("-h", "Print this message");
        put("-t", "The table name");
        put("-v", "Show verbose log");
        put("-r", "Remove not exists items of table");
        put("-i", "Import from a csv file");
        put("-x", "Export to a csv file");
        put("-e", "The excluded paths.");
    }};

    public static void main(String[] args) throws IOException {
        String options = "hvrt:e:i:x:";
        GetOpt getOpt = new GetOpt(args, options);
        int c;
        String tableName = null;
        HandlerPath hp = HandlerPath.getInstance();
        boolean remoteNotExistsItems = false;
        boolean verbose = false;
        String importCsv = null;
        String exportCsv = null;
        while ((c = getOpt.getNextOption()) != -1) {
            switch (c) {
                case 'h':
                    getOpt.printUsage("MakeIndex", description);
                    System.exit(0);
                    break;
                case 't':
                    tableName = getOpt.getOptionArg();
                    break;
                case 'r':
                    remoteNotExistsItems = true;
                    break;
                case 'v':
                    verbose = true;
                    break;
                case 'i':
                    importCsv = getOpt.getOptionArg();
                    break;
                case 'x':
                    exportCsv = getOpt.getOptionArg();
                    break;
                case 'e':
                    hp.addExcludePath(getOpt.getOptionArg());
                    break;
            }
        }


        if (verbose) {
            Log.set(Log.LEVEL_DEBUG);
        } else {
            Log.set(Log.LEVEL_INFO);
        }


        if (!StringUtils.isNullOrEmpty(importCsv)) {
            importCsv(tableName, importCsv);
        }

        if (!StringUtils.isNullOrEmpty(exportCsv)) {
            exportCsv(tableName, exportCsv);
        }

        if (remoteNotExistsItems) {
            removeNotExistItems(tableName);

        }

        String[] cmdArgs = getOpt.getCmdArgs(); // as include paths
        if (cmdArgs != null && cmdArgs.length > 0) {
            hp.addIncludePaths(cmdArgs);
            hp.addExcludePaths(".m2", ".git", ".svn", ".cvs");
            if (StringUtils.isNullOrEmpty(tableName)) {
                getOpt.printUsage("MakeIndex", description);
                System.exit(1);
            }
            makeIndex(tableName.trim());
        }

    }

    private static void importCsv(String tableName, String importCsv) {
        Objects.requireNonNull(tableName, "The tableName should not be empty.");
        Log.info("MIDX", "Import from csv " + importCsv);
        Path p = Paths.get(importCsv);
        if (!Files.isRegularFile(p)) {
            Log.error("MIDX", "The csv file is not found. " + importCsv);
            return;
        }
        DataSourceFactory.createTable(tableName);
        FileIndexDao dao = new FileIndexDao();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try (CsvReader csvReader = new CsvReader(importCsv, Letters.COMMA, StandardCharsets.UTF_8)) {
            if (!csvReader.readHeaders()) {
                Log.error("Can't read headers");
                return;
            }
            int i = 0;
            while (csvReader.readRecord()) {
                String filePath = csvReader.get("file_path");
                String fileExt = csvReader.get("file_ext");
                String md5sum = csvReader.get("md5sum");
                String lastModified = csvReader.get("last_modified");
                String fileSize = csvReader.get("file_size");
                FileInfo fi = new FileInfo();
                fi.setFilePath(filePath);
                fi.setFileExt(fileExt);
                fi.setMd5sum(md5sum);
                fi.setLastModified(new Timestamp(sdf.parse(lastModified).getTime()));
                fi.setFileSize(Long.parseLong(fileSize));
                if (dao.exists(tableName, filePath)) {
                    dao.updateByPath(tableName, fi);
                } else {
                    dao.insert(tableName, fi);
                }
                i++;
            }
            Log.info("MIDX", "Import " + i + " files.");
            Log.info("MIDX", "Import from CSV DONE.");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        }

    }

    private static void exportCsv(String tableName, String exportCsv)
            throws IOException {
        Objects.requireNonNull(tableName, "The tableName should not be empty.");
        Log.info("MIDX", "Export csv to " + exportCsv);
        Path p = Paths.get(exportCsv);
        if (!Files.isDirectory(p.getParent())) {
            Files.createDirectories(p.getParent());
            return;
        }
        FileIndexDao dao = new FileIndexDao();
        List<FileInfo> fileInfos = dao.loadTable(tableName);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try (CsvWriter csvWriter = new CsvWriter(exportCsv, Letters.COMMA, StandardCharsets.UTF_8)) {
            csvWriter.writeRecord(new String[]{"file_path", "file_ext", "md5sum", "last_modified", "file_size"});

            for (FileInfo fi : fileInfos) {
                csvWriter.writeRecord(new String[]{
                        fi.getFilePath(), fi.getFileExt(), fi.getMd5sum(),
                        sdf.format(fi.getLastModified()), "" + fi.getFileSize()
                });
            }
            Log.info("MIDX", "Export to CSV DONE.");
        }

    }

    private static void removeNotExistItems(String tableName) {
        Objects.requireNonNull(tableName, "The tableName should not be empty.");
        FileIndexDao dao = new FileIndexDao();
        List<FileInfo> fileInfos = dao.loadTable(tableName);
        fileInfos.parallelStream().forEach(fi -> {
            Path path = Paths.get(fi.getFilePath());
            Log.debug("MIDX", "Processing " + path);
            if (!Files.exists(path)) {
                Log.info(path + " is not exists.");
                dao.delete(tableName, fi.getFilePath());
            }
        });
        Log.info("MIDX", "Remove not exists items DONE.");
    }

    private static void makeIndex(String tableName) throws IOException {
        DataSourceFactory.createTable(tableName);
        HandlerPath hp = HandlerPath.getInstance();
        Set<String> includePath = hp.getIncludePath();
        Set<String> excludePath = hp.getExcludepath();
        for (String path : includePath) {
            Log.info("MIDX", "Processing directory: " + path);
            Path fileDir = Paths.get(path);
            MyFileVisitor visitor = new MyFileVisitor(tableName);
            visitor.setIgnoreDirs(excludePath.toArray(new String[excludePath.size()]));
            long a = System.currentTimeMillis();
            Files.walkFileTree(fileDir, visitor);
            String msg = String.format("Process %s %d files, cost %s ms.%n",
                    path, visitor.getFileCnt(), (System.currentTimeMillis() - a));
            Log.info("MIDX", msg);
            Log.info("MIDX", "Save to " + tableName);
        }
        Log.info("MIDX", "Make index DONE.");
    }
}
