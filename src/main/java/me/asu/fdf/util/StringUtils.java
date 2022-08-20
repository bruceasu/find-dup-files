package me.asu.fdf.util;

public class StringUtils {
    /**
     * 将一个字节数变成人类容易识别的显示字符串，比如 1.5M 等
     *
     * @param size 字节数
     * @param thousandUnit 千的单位，可能为 1024 或者 1000
     * @return 人类容易阅读的字符串
     */
    private static String formatSizeForRead(long size, double thousandUnit) {
        if (size < thousandUnit) {
            return String.format("%d BS", size);
        }
        double n = size / thousandUnit;
        if (n < thousandUnit) {
            return String.format("%5.2f KB", n);
        }
        n = n / thousandUnit;
        if (n < thousandUnit) {
            return String.format("%5.2f MB", n);
        }
        n = n / thousandUnit;
        return String.format("%5.2f GB", n);
    }

    /**
     * @see #formatSizeForRead(long, double)
     */
    public static String formatSizeForReadBy1024(long size) {
        return formatSizeForRead(size, 1024);
    }

    /**
     * @see #formatSizeForRead(long, double)
     */
    public static String formatSizeForReadBy1000(long size) {
        return formatSizeForRead(size, 1000);
    }
}
