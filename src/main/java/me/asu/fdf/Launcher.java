package me.asu.fdf;

import java.io.IOException;

public class Launcher {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }

        String[] newArgs = new String[args.length - 1];
        if (newArgs.length > 0) {
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        }
        switch (args[0]) {
            case "index":
                MakeIndex.main(newArgs);
                break;
            case "dup":
                FindDupFiles.main(newArgs);
                break;
            case "diff":
                Difference.main(newArgs);
                break;
            case "db":
                DB.main(newArgs);
                break;
            case "help":
                help(newArgs);
            default:
                usage();
                System.exit(1);
        }
    }

    private static void help(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }
        try {
            switch (args[0]) {
                case "index":
                    MakeIndex.main(new String[]{"-h"});
                    break;
                case "dup":
                    FindDupFiles.main(new String[]{"-h"});
                    break;
                case "diff":
                    Difference.main(new String[]{"-h"});
                    break;
                case "db":
                    DB.main(new String[]{"-h"});
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void usage() {
        System.out.println("usage: Launcher <command> [params]");
        System.out.println("commands:");
        System.out.println("\tindex Create the index");
        System.out.println("\tdup Find duplicated files");
        System.out.println("\tdiff Find out the difference between 2 tables.");
        System.out.println("\tdb DB operations.");
    }
}
