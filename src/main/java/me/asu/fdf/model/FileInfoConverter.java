package me.asu.fdf.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import me.asu.fdf.util.Md5Utils;

/**
 * 文件对象转换Thing对象的辅助类
 */

public class FileInfoConverter {

    public static FileInfo convert(Path file) throws IOException {
        FileInfo fi = new FileInfo();
        fi.setFilePath(file.toAbsolutePath().toString());
        /**
         * 目录 -> *
         * 文件 -> 有扩展名，通过扩展名获取FileType
         *         无扩展，*
         */
        String name = file.getFileName().toString();
        int index = name.lastIndexOf(".");
        String extend = "*";
        if (index > 0 && (index + 1) < name.length()) {
            extend = name.substring(index + 1);
        }
        fi.setFileExt(extend);

        String s  = Md5Utils.getInstance().md5sum(file);
        fi.setMd5sum(s);
        fi.setFileSize(Files.size(file));
        fi.setLastModified(new Timestamp(Files.getLastModifiedTime(file).toMillis()));
        fi.setFilePath(file.toAbsolutePath().toString());
        return fi;
    }

}
