
create table if not exists $table_name
(
    file_path varchar(1024) not null comment 'path',
    file_ext varchar (64) not null comment 'file extend',
    md5sum varchar(32) comment 'the md5sum of file',
    last_modified timestamp comment 'the last modified time of file',
    file_size bigint comment 'the file size'
);
create index if not exists $table_name_index_md5 on $table_name(md5sum);
create index if not exists $table_name_index_path on $table_name(file_path);

-- alter table fileInfo add (name_lower varchar(100) as lower(name));
-- create index ix_element_add_low on fileInfo (name_lower);
-- select * from fileInfo e where locate (lower(?), lower(e.name));
-- select * from fileInfo e where locate (lower(?), name_lower);