package me.asu.fdf;

public record FileFp(String path, long size, long mtime, String quickHash, String sourceDisk,
                Long groupId) {
}
