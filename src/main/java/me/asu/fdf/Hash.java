package me.asu.fdf;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
/**
 * =========================
 * Hash utils
 * =========================
 */
class Hash {

    static String quickHash(String path, long size) {
        try (FileChannel ch = FileChannel.open(Paths.get(path), StandardOpenOption.READ)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            read(ch, md, 0);
            read(ch, md, Math.max(0, size / 2 - Dedup.CHUNK / 2));
            read(ch, md, Math.max(0, size - Dedup.CHUNK));

            return hex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    static void read(FileChannel ch, MessageDigest md, long pos) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(Dedup.CHUNK);
        ch.position(pos);
        int n = ch.read(buf);
        if (n > 0) {
            buf.flip();
            byte[] tmp = new byte[n];
            buf.get(tmp);
            md.update(tmp);
        }
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b)
            sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
