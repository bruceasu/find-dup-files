package me.asu.fdf;

import me.asu.fdf.dao.DataSourceFactory;
import me.asu.fdf.dao.FileIndexDao;
import me.asu.fdf.model.FileInfo;
import me.asu.fdf.util.GetOpt;
import me.asu.log.Log;
import org.h2.util.StringUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static me.asu.fdf.util.StringUtils.formatSizeForReadBy1024;

public class DB {

    private static Map<String, String> description = new HashMap<String, String>() {{
        put("-r", "Delete an item by file_path");
        put("-h", "Print this message.");
        put("-l", "List the tables.");
        put("-p", "The file path");
        put("-s", "Show table items");
        put("-d", "Drop table");
        put("-t", "The tableName");
        put("-v", "Verbose");
        put("-x", "The file path is a pattern");
        put("-c", "Clean dup file index");
        put("-C", "COMPACT");
    }};

    public static void main(String[] args) throws IOException {
        String options = "hlsdp:t:xvcC";
        GetOpt getOpt = new GetOpt(args, options);
        int c;
        String path = null, tableName = null;
        boolean verbose = false, del = false, st = false, drop = false, compact = false,
                pathIsPattern = false, lt = false, toClean = false;
        while ((c = getOpt.getNextOption()) != -1) {
            switch (c) {
                case 'h':
                    getOpt.printUsage("DB", description);
                    System.exit(0);
                    break;
                case 'p': path = getOpt.getOptionArg();break;
                case 't': tableName = getOpt.getOptionArg();break;
                case 'v': verbose = true;break;
                case 'r': del = true;break;
                case 'd': drop = true;break;
                case 'x': pathIsPattern = true;break;
                case 's': st = true;break;
                case 'l': lt = true;break;
                case 'c': toClean = true;break;
                case 'C': compact = true;break;
            }
        }

        setVerbose(verbose);

        if (lt) listTables();
        if (st) showTable(tableName, path, pathIsPattern);
        if (del) delete(tableName, path, pathIsPattern);
        if (toClean) removeDupIndex(tableName);
        if (drop) dropTable(tableName);
        if (compact) DataSourceFactory.compact();
    }

    private static void dropTable(String tableName) {
        Objects.requireNonNull(tableName);
        DataSourceFactory.drop(tableName);
    }

    private static void removeDupIndex(String tableName) {
        Objects.requireNonNull(tableName);
        FileIndexDao dao = new FileIndexDao();
        dao.removeDupIndex(tableName);
    }

    private static void delete(String tableName, String path, boolean pathIsPattern) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(path);
        FileIndexDao dao = new FileIndexDao();
        if (pathIsPattern) {
            List<FileInfo> fileInfos = dao.loadTable(tableName);
            if (fileInfos == null || fileInfos.isEmpty()) {
                System.out.println("There's no items in " + tableName);
                return;
            }
            Iterator<FileInfo> iterator = fileInfos.iterator();
            while (iterator.hasNext()) {
                FileInfo fi = iterator.next();
                if (fi.getFilePath().matches(path)) {
                    dao.delete(tableName, fi.getFilePath());
                }
            }
        } else {
            dao.delete(tableName, path);
        }
    }

    private static void showTable(String tableName, String path, boolean pathIsPattern) throws IOException {
        Objects.requireNonNull(tableName);
        FileIndexDao dao = new FileIndexDao();
        List<FileInfo> fileInfos = dao.loadTable(tableName);
        if (fileInfos == null || fileInfos.isEmpty()) {
            System.out.println("There's no items in " + tableName);
            return;
        }
        if (!StringUtils.isNullOrEmpty(path)) {
            Iterator<FileInfo> iterator = fileInfos.iterator();
            while (iterator.hasNext()) {
                FileInfo fi = iterator.next();
                if (pathIsPattern) {
                    if (!fi.getFilePath().contains(path)) iterator.remove();
                } else {
                    if (!path.equals(fi.getFilePath())) iterator.remove();
                }
            }
        }
        int i = 0;
        System.out.println();
        String fmt = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        int pageSize = 20;
        do {
            int j = i + pageSize;
            if (j > fileInfos.size()) j = fileInfos.size();
            List<FileInfo> ls = fileInfos.subList(i, j);
            // file_path, last_modified, file_size, md5sum
            int colSize[] = new int[]{10, fmt.length(), 9, 32};
            for (FileInfo fi : ls) {
                String filePath = fi.getFilePath();
                int length = getPathDisplayLength(filePath);
                if (colSize[0] < length) {
                    colSize[0] = length;
                }
            }

            // title
            System.out.println("----------------------------------------------------------------------------");
            System.out.printf("%8s | %-" + colSize[0] + "s | %-" + colSize[1] + "s | %-" + colSize[2] + "s | %-" + colSize[3] + "s |%n",
                    "  No.", "file_path", "  last_modified", "file_size", "             md5sum");
            for (int k = 0, lsSize = ls.size(); k < lsSize; k++) {
                FileInfo fi = ls.get(k);
                int idx = i + k + 1;
                System.out.printf("%8d | ", idx);
                String filePath = fi.getFilePath();
                System.out.printf(filePath);
                int length = getPathDisplayLength(filePath);
                int delta = colSize[0] - length;
                while (delta-- > 0) {
                    System.out.printf(" ");
                }
                System.out.printf(" | %-" + colSize[1] + "s | %" + colSize[2] + "s | %-" + colSize[3] + "s |%n",
                        sdf.format(fi.getLastModified()), formatSizeForReadBy1024(fi.getFileSize()), fi.getMd5sum());
            }

            i = j;
            if (i < fileInfos.size()) {
                System.out.println("============================================================================");
                String s = prompt("Continue/Quit?");
                if ("q".equalsIgnoreCase(s)) {
                    break;
                }
            }
        } while (i < fileInfos.size());
    }

    private static int getPathDisplayLength(String filePath) {
        int length = 0;
        char[] chars = filePath.toCharArray();
        for (char c : chars) {
            if (c >= 0x4E00) { // 快速检测，不是最精准
                length += 2;

            } else {
                length++;
            }
        }
        return length;
    }

    private static String prompt(String prompt) throws IOException {
        Console console = System.console();
        if (console == null) {
            System.out.println(prompt);
            char read = (char) System.in.read();
            return new String(new char[]{read});
        } else {
            return console.readLine();
        }
    }

    private static void listTables() {
        System.out.println("Tables:");
        List<String> strings = DataSourceFactory.listTables();
        for (String string : strings) {
            System.out.println(string);
        }
        System.out.println("============================================================================");
    }

    private static void setVerbose(boolean verbose) {
        if (verbose) {
            Log.set(Log.LEVEL_DEBUG);
        } else {
            Log.set(Log.LEVEL_INFO);
        }
    }
}
