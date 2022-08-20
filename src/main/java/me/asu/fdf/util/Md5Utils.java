package me.asu.fdf.util;

import me.asu.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Utils {

    private static Holder holder = new Holder();

    private Md5Utils() {

    }

    public static Md5Utils getInstance() {
        return holder.instance;
    }

    public String md5sum(Path filePath) throws IOException {
        long startTs = System.nanoTime();
        try {
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                try (InputStream is = Files.newInputStream(filePath)) {
                    MessageDigest md5 = null;
                    md5 = MessageDigest.getInstance("MD5");
                    byte[] data = new byte[8094];
                    int read;
                    do {
                        read = is.read(data);
                        md5.update(data);
                    } while (read == 8094);
                    byte[] digest = md5.digest();
                    StringBuffer hexString = new StringBuffer();
                    String strTemp;
                    for (int i = 0; i < digest.length; i++) {
                        // byteVar &
                        // 0x000000FF的作用是，如果digest[i]是负数，则会清除前面24个零，正的byte整型不受影响。
                        // (...) | 0xFFFFFF00的作用是，如果digest[i]是正数，则置前24位为一，
                        // 这样toHexString输出一个小于等于15的byte整型的十六进制时，倒数第二位为零且不会被丢弃，这样可以通过substring方法进行截取最后两位即可。
                        strTemp = Integer.toHexString((digest[i] & 0x000000FF) | 0xFFFFFF00).substring(6);
                        hexString.append(strTemp);
                    }
                    return hexString.toString();

                } catch (NoSuchAlgorithmException e) {
                    // wrap
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("This isn't a regular file： " + filePath);
            }
        } finally {
            Log.debug(filePath + " md5sum cost " + (System.nanoTime() - startTs) + " ms.");
        }
    }

    public String md5sum(byte[] data) {

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(data);
            byte[] digest = md5.digest();
            StringBuffer hexString = new StringBuffer();
            String strTemp;
            for (int i = 0; i < digest.length; i++) {
                // byteVar &
                // 0x000000FF的作用是，如果digest[i]是负数，则会清除前面24个零，正的byte整型不受影响。
                // (...) | 0xFFFFFF00的作用是，如果digest[i]是正数，则置前24位为一，
                // 这样toHexString输出一个小于等于15的byte整型的十六进制时，倒数第二位为零且不会被丢弃，这样可以通过substring方法进行截取最后两位即可。
                strTemp = Integer.toHexString(
                        (digest[i] & 0x000000FF) | 0xFFFFFF00).substring(6);
                hexString.append(strTemp);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // wrap
            throw new RuntimeException(e);
        }
    }

    public String md5sum(String str) {
        return this.md5sum(str, StandardCharsets.UTF_8.name());
    }

    public String md5sum(String str, String encode) {
        try {
            if (str == null || str.trim().isEmpty()) {
                return null;
            }
            if (encode == null || encode.isEmpty()) {
                encode = StandardCharsets.ISO_8859_1.name();
            }
            return md5sum(str.getBytes(encode));
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

    static class Holder {

        Md5Utils instance = new Md5Utils();
    }


}