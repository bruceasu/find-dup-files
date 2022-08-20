package me.asu.fdf.model;

import java.sql.Timestamp;
import lombok.Data;

@Data
public class FileInfo {

    /*文件路径*/
    private String filePath;
    /*文件类型*/
    private String fileExt;

    private String    md5sum;
    private Timestamp lastModified;
    private long      fileSize;

}

