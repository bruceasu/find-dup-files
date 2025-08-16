/*
| 字段          | 说明                       |
| ----------- | ------------------------ |
| path        | 绝对路径（主键）                 |
| size        | 文件大小                     |
| mtime       | 修改时间                     |
| quick_hash  | 3 段 hash                 |
| full_hash   | 可空                       |
| source_disk | `A-driver / local / xxx` |
| group_id    | report 阶段生成              |
*/

CREATE TABLE IF NOT EXISTS file_fp (
    path TEXT PRIMARY KEY,
    size INTEGER NOT NULL,
    mtime INTEGER NOT NULL,
    quick_hash TEXT,
    full_hash TEXT,
    source_disk TEXT,
    group_id INTEGER,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_size_qh ON file_fp(size, quick_hash);
CREATE INDEX IF NOT EXISTS idx_group ON file_fp(group_id);
CREATE INDEX IF NOT EXISTS idx_fullhash ON file_fp(full_hash);