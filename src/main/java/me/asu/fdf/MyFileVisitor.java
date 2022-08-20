package me.asu.fdf;

import lombok.Getter;
import me.asu.fdf.dao.FileIndexDao;
import me.asu.fdf.model.FileInfo;
import me.asu.fdf.model.FileInfoConverter;
import me.asu.fdf.util.Md5Utils;
import me.asu.log.Log;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Timestamp;

public class MyFileVisitor extends SimpleFileVisitor<Path> {

    private String tableName;
    private int folderCnt;
    @Getter
    private int fileCnt;
    private String[] ignoreDirs = {".m2", ".git", ".svn", ".cvs", "node_modules", ".idea"};
    HandlerPath paths = HandlerPath.getInstance();
    FileIndexDao dao = new FileIndexDao();

    public MyFileVisitor(String tableName) {
        this.tableName = tableName;
    }

    public String[] getIgnoreDirs() {
        return ignoreDirs;
    }

    public void setIgnoreDirs(String[] ignoreDirs) {
        this.ignoreDirs = ignoreDirs;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir,
                                             BasicFileAttributes attrs) throws IOException {

        String name = dir.toFile().getName();
        for (String d : ignoreDirs) {
            if (name.equals(d)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {
        // 访问文件后调用
        while (attrs.isRegularFile()) {
            long size = Files.size(file);
            FileTime lastModifiedTime = Files.getLastModifiedTime(file);
            long l = lastModifiedTime.toMillis();
            String filePath = file.toAbsolutePath().toString();
            if (dao.exists(tableName, filePath)) {
                FileInfo fi = dao.getByPath(tableName, filePath);
                if (fi.getFileSize() != size
                        || fi.getLastModified().getTime() != l) {
                    // different
                    String s = Md5Utils.getInstance().md5sum(file);
                    fi.setMd5sum(s);
                    fi.setFileSize(size);
                    fi.setLastModified(new Timestamp(l));
                    dao.updateByPath(tableName, fi);
                } else {
                    break;
                }
            }

            FileInfo fi = FileInfoConverter.convert(file);
            dao.insert(tableName, fi);
            break;
        }
        fileCnt++;
        if (fileCnt % 1000 == 0) {
            Log.info("Processed " + fileCnt + " files.");
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
            throws IOException {
        // 文件不可访问时调用
        System.out.println(exc.getMessage());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exec)
            throws IOException {
        // 访问文件夹之前调用
        //System.out.println("Just visited " + dir);
        folderCnt++;
        if (folderCnt % 1000 == 0) {
            Log.info("Processed " + folderCnt + " directories.");
        }
        return FileVisitResult.CONTINUE;
    }

}