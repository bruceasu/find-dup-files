package me.asu.fdf.model;

import java.sql.Timestamp;
import lombok.Data;

/*
*Condition： 检索条件的模型类型
*/
@Data
public class Condition {
    private String tableName;
    /**
     * 文件名
     */
    private String filePath;

    /**
     * 文件类型
     */
    private String fileExt;

    private String md5sum;

    private Timestamp lastModified;

    private Long fileSize;

    /**
     * 限制的数量
     */
    private Integer limit;


    public String orderBy;

}
