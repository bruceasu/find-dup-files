package me.asu.fdf;

public  record FileFpRow(String path, long size, long mtime, String sourceDisk) {
    }