package me.asu.fdf.dao;


import me.asu.fdf.model.Condition;
import me.asu.fdf.model.FileInfo;
import me.asu.log.Log;
import org.h2.util.StringUtils;

import java.sql.*;
import java.util.*;

public class FileIndexDao {

    public void insert(String tableName, FileInfo fileInfo) {
        //JDBC操作
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DataSourceFactory.getConnection();
            String sql = "insert into " + tableName
                    + "(file_path, file_ext, md5sum, last_modified, file_size) values(?,?,?,?,?)";
            Log.debug("SQL: " + sql);
            Log.debug("PARAMS:" + fileInfo);
            statement = connection.prepareStatement(sql);
            //预编译命令中SQL的占位符赋值
            statement.setString(1, fileInfo.getFilePath());
            statement.setString(2, fileInfo.getFileExt());
            statement.setString(3, fileInfo.getMd5sum());
            statement.setTimestamp(4, fileInfo.getLastModified());
            statement.setLong(5, fileInfo.getFileSize());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseResource(null, statement, connection);
        }
    }

    public void deleteDir(String tableName, String fullPathDir) {
        if (fullPathDir == null || fullPathDir.trim().isEmpty()) {
            return;
        }
        fullPathDir = fullPathDir.trim().replaceAll("'", "''");
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DataSourceFactory.getConnection();
            String sql = "delete from " + tableName + " where file_path '"
                    + fullPathDir + "%'";
            Log.debug("SQL: " + sql);

            statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseResource(null, statement, connection);
        }
    }

    public void delete(String tableName, String path) {
        /**
         * thing -> path => D:\a\b\hello.java
         * thing -> path => D:\a\b
         *                  D:\a\ba
         * like path%
         * = 最多删除一个，绝对匹配
         */
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DataSourceFactory.getConnection();
            String sql = "delete from " + tableName + " where file_path = ?";
            Log.debug("SQL: " + sql);
            Log.debug("PARAMS: " + path);
            statement = connection.prepareStatement(sql);
            statement.setString(1, path);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseResource(null, statement, connection);
        }
    }

    public int deleteByExt(String tableName, String ext) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DataSourceFactory.getConnection();
            String sql = "delete from " + tableName + " where file_ext = ?";
            Log.debug("SQL: " + sql);
            Log.debug("PARAMS: " + ext);
            statement = connection.prepareStatement(sql);
            statement.setString(1, ext);
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            releaseResource(null, statement, connection);
        }
    }

    public List<FileInfo> loadTable(String tableName) {
        if (StringUtils.isNullOrEmpty(tableName)) {
            return Collections.emptyList();
        }

        Condition c = new Condition();
        c.setOrderBy("order by file_path");
        c.setTableName(tableName);
        List<FileInfo> query = query(c);
        if (query == null) {
            return Collections.emptyList();
        }
        return query;
    }

    public List<FileInfo> query(Condition condition) {
        List<FileInfo> fileInfos = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            Objects.requireNonNull(condition);
            StringBuilder sb = new StringBuilder();
            sb.append("select * from ").append(condition.getTableName());
            sb.append(" where 1=1 ");
            //  sb.append("locate (lower(?), name_lower)");
            List<Object> params = new ArrayList<>();
            if (!StringUtils.isNullOrEmpty(condition.getFilePath())) {
                sb.append(" and file_path = ?");
                params.add(condition.getFilePath());
            }
            if (!StringUtils.isNullOrEmpty(condition.getFileExt())) {
                sb.append(" and file_ext = ?");
                params.add(condition.getFileExt());
            }
            if (!StringUtils.isNullOrEmpty(condition.getMd5sum())) {
                sb.append(" and md5sum = ?");
                params.add(condition.getMd5sum());
            }
            if (condition.getLastModified() != null) {
                sb.append(" and last_modified = ?");
                params.add(condition.getLastModified());
            }
            if (condition.getFileSize() != null) {
                sb.append(" and file_size = ?");
                params.add(condition.getFileSize());
            }
            if (!StringUtils.isNullOrEmpty(condition.getOrderBy())) {
                sb.append(" ").append(condition.getOrderBy());
            }
            if (condition.getLimit() != null) {
                sb.append(" limit ").append(condition.getLimit());
            }
            Log.debug("SQL: " + sb.toString());
            Log.debug("PARAMS: " + params);
            connection = DataSourceFactory.getConnection();
            statement = connection.prepareStatement(sb.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            resultSet = statement.executeQuery();
            //处理结果
            while (resultSet.next()) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setFilePath(resultSet.getString("file_path"));
                fileInfo.setMd5sum(resultSet.getString("md5sum"));
                fileInfo.setFileExt(resultSet.getString("file_ext"));
                fileInfo.setLastModified(resultSet.getTimestamp("last_modified"));
                fileInfo.setFileSize(resultSet.getLong("file_size"));
                fileInfos.add(fileInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseResource(resultSet, statement, connection);
        }

        return fileInfos;
    }

    public FileInfo getByPath(String tableName, String path) {
        if (StringUtils.isNullOrEmpty(tableName)
                || StringUtils.isNullOrEmpty(path)) {
            return null;
        }

        Condition c = new Condition();
        c.setTableName(tableName);
        c.setFilePath(path);
        c.setLimit(1);
        List<FileInfo> query = query(c);
        if (query == null || query.isEmpty()) {
            return null;
        }
        return query.get(0);
    }

    public void updateByPath(String tableName, FileInfo info) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(info);
        Objects.requireNonNull(info.getFilePath());
        StringBuilder builder = new StringBuilder("update ").append(tableName)
                .append(" set ");
        StringBuilder setSql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (StringUtils.isNullOrEmpty(info.getFileExt())) {
            setSql.append(" file_ext = null,");
        } else {
            setSql.append(" file_ext = ?,");
            params.add(info.getFileExt());
        }
        if (StringUtils.isNullOrEmpty(info.getMd5sum())) {
            setSql.append(" md5sum = null,");
        } else {
            setSql.append(" md5sum = ?,");
            params.add(info.getMd5sum());
        }
        if (info.getLastModified() != null) {
            setSql.append(" last_modified = ?,");
            params.add(info.getLastModified());
        } else {
            setSql.append(" last_modified = ?,");
            params.add(new Timestamp(System.currentTimeMillis()));
        }
        setSql.append(" file_size = ?");
        params.add(info.getFileSize());

        builder.append(setSql);
        builder.append(" where file_path = ?");
        params.add(info.getFilePath());
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            Log.debug("SQL: " + builder);
            Log.debug("PARAMS: " + params);
            connection = DataSourceFactory.getConnection();
            statement = connection.prepareStatement(builder.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseResource(resultSet, statement, connection);
        }

    }

    public boolean exists(String tableName, String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        path = path.replaceAll("\'", "\'\'");
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("select count(1) from ").append(tableName);
            sb.append(" where ");
            //采用模糊匹配
            //前迷糊
            //后模糊
            //前后模糊
            sb.append(" file_path = '").append(path).append("'");

            Log.debug("SQL: " + sb);
            connection = DataSourceFactory.getConnection();
            statement = connection.prepareStatement(sb.toString());
            resultSet = statement.executeQuery();
            //处理结果
            if (resultSet.next()) {
                int anInt = resultSet.getInt(1);
                return anInt > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseResource(resultSet, statement, connection);
        }
        return false;
    }

    /**
     * 全库有多个相同文件时，清除多个，
     * 通常是由于windows系统不区分大小写导致的
     */
    public void removeDupIndex(String tableName) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            String sql = "select file_path from " + tableName;
            connection = DataSourceFactory.getConnection();
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            //处理结果
            Map<String, List<String>> m = new HashMap<>();
            while (resultSet.next()) {
                String path = resultSet.getString(1);
                if (path.length() >= 2 && path.charAt(1) == ':') {
                    // windows file
                    String name = path.toLowerCase();
                    m.putIfAbsent(name, new LinkedList<>());
                    m.get(name).add(path);
                }
            }
            m.forEach((n, ps) -> {
                if (ps.size() == 1) return;
                for (int i = 1; i < ps.size(); i++) {
                    String s = ps.get(i);
                    delete(tableName, s);
                }
            });
        } catch (SQLException e) {
           Log.error("DB", e.getMessage(), e);
        } finally {
            releaseResource(resultSet, statement, connection);
        }
    }

    private void releaseResource(ResultSet resultSet,
                                 Statement statement,
                                 Connection connection) {
        DataSourceFactory.closeQuietly(resultSet);
        DataSourceFactory.closeQuietly(statement);
        DataSourceFactory.closeQuietly(connection);
    }
}
