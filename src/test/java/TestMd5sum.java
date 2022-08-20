import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import me.asu.fdf.util.Md5Utils;

public class TestMd5sum {

    public static void main(String[] args) throws Exception {
        Path filePath = Paths.get("D:\\99_backup\\os\\linux\\EndeavourOS_Artemis_neo_22_7.iso");
//        java(filePath);
        c(filePath);
    }

    private static void c(Path filePath)
    throws IOException, InterruptedException {
        long t1 = System.currentTimeMillis();
        String cmd = "md5sum.exe " + filePath.toString();
        Process exec = Runtime.getRuntime().exec(cmd);
        exec.waitFor();
        long t2 = System.currentTimeMillis();
        System.out.println("C cost: " + (t2-t1) + " ms.");
    }
    private static void java(Path filePath) throws IOException {
        long t1 = System.currentTimeMillis();
        String x= Md5Utils.getInstance().md5sum(filePath);
        long t2 = System.currentTimeMillis();
        System.out.println("Java cost: " + (t2-t1) + " ms.");
    }
}
