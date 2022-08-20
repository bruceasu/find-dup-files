/*
 * When you don't want to use jcommander, you can use this simple argument parser.
 * <p>jcommander:
 * <pre><code>
 * <dependency>
 *   <groupId>com.beust</groupId>
 *   <artifactId>jcommander</artifactId>
 *   <version>1.78</version>
 * </dependency>
 * </code></pre>
 * </p>
 */
/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: GetOpt.java,v 1.2.4.1 2005/08/31 11:46:04 pvedula Exp $
 */

package me.asu.fdf.util;

import java.lang.Character.UnicodeBlock;
import java.util.*;


/**
 * GetOpt is a Java equivalent to the C getopt() library function
 * discussed in man page getopt(3C). It provides command line
 * parsing for Java applications. It supports the most rules of the
 * command line standard (see man page intro(1)) including stacked
 * options such as '-sxm' (which is equivalent to -s -x -m); it
 * handles special '--' option that signifies the end of options.
 * Additionally this implementation of getopt will check for
 * mandatory arguments to options such as in the case of
 * '-d <file>' it will throw a MissingOptArgException if the
 * option argument '<file>' is not included on the commandline.
 * getopt(3C) does not check for this.
 *
 * @author G Todd Miller
 */
public class GetOpt {

    public GetOpt(String[] args, String optString) {
        theOptions = new ArrayList();
        int currOptIndex = 0;
        theCmdArgs       = new ArrayList();
        theOptionMatcher = new OptionMatcher(optString);
        // fill in the options list
        for (int i = 0; i < args.length; i++) {
            String token       = args[i];
            int    tokenLength = token.length();
            if (token.equals("--")) {         // end of opts
                currOptIndex = i + 1;         // set index of first operand
                break;                      // end of options
            } else if (token.startsWith("-") && tokenLength == 2) {
                // simple option token such as '-s' found
                theOptions.add(new Option(token.charAt(1)));
                currOptIndex = i + 1;
            } else if (token.startsWith("-") && tokenLength > 2) {
                // stacked options found, such as '-shm'
                // iterate thru the tokens after the dash and
                // add them to theOptions list
                for (int j = 1; j < tokenLength; j++) {
                    theOptions.add(new Option(token.charAt(j)));
                }
                currOptIndex = i + 1;
            } else if (!token.startsWith("-")) {
                // case 1- there are not options stored yet therefore
                // this must be an command argument, not an option argument
                if (theOptions.size() == 0) {
                    currOptIndex = i;
                    break;              // stop processing options
                } else {
                    // case 2-
                    // there are options stored, check to see if
                    // this arg belong to the last arg stored
                    int indexoflast = 0;
                    indexoflast = theOptions.size() - 1;
                    Option op       = (Option) theOptions.get(indexoflast);
                    char   opLetter = op.getArgLetter();
                    if (!op.hasArg() && theOptionMatcher.hasArg(opLetter)) {
                        op.setArg(token);
                        currOptIndex = i + 1;
                    } else {
                        // case 3 -
                        // the last option stored does not take
                        // an argument, so again, this argument
                        // must be a command argument, not
                        // an option argument
                        currOptIndex = i;
                        break;                  // end of options
                    }
                }
            }// end option does not start with "-"
        } // end for args loop

        //  attach an iterator to list of options
        theOptionsIterator = theOptions.listIterator();

        // options are done, now fill out cmd arg list with remaining args
        for (int i = currOptIndex; i < args.length; i++) {
            String token = args[i];
            theCmdArgs.add(token);
        }
    }


    /**
     * debugging routine to print out all options collected
     */
    public void printOptions() {
        for (ListIterator it = theOptions.listIterator(); it.hasNext(); ) {
            Option opt = (Option) it.next();
            System.out.print("OPT =" + opt.getArgLetter());
            String arg = opt.getArgument();
            if (arg != null) {
                System.out.print(" " + arg);
            }
            System.out.println();
        }
    }

    public Map<Character, Option> getOptions() {
        List<Option>           list = new ArrayList<>(theOptions);
        Map<Character, Option> map  = new TreeMap<>();
        list.forEach(o -> {
            map.put(o.getArgLetter(), o);
        });

        return map;
    }

    /**
     * gets the next option found in the commandline. Distinguishes
     * between two bad cases, one case is when an illegal option
     * is found, and then other case is when an option takes an
     * argument but no argument was found for that option.
     * If the option found was not declared in the optString, then
     * an IllegalArgumentException will be thrown (case 1).
     * If the next option found has been declared to take an argument,
     * and no such argument exists, then a MissingOptArgException
     * is thrown (case 2).
     *
     * @return int - the next option found.
     * @throws IllegalArgumentException, MissingOptArgException.
     */
    public int getNextOption() throws IllegalArgumentException {
        int retval = -1;
        if (theOptionsIterator.hasNext()) {
            theCurrentOption = (Option) theOptionsIterator.next();
            char    c             = theCurrentOption.getArgLetter();
            boolean shouldHaveArg = theOptionMatcher.hasArg(c);
            String  arg           = theCurrentOption.getArgument();
            if (!theOptionMatcher.match(c)) {
                throw (new IllegalArgumentException("Unknown " + c));
            } else if (shouldHaveArg && (arg == null)) {
                throw (new IllegalArgumentException(
                        c + " should have an argument."));
            }
            retval = c;
        }
        return retval;
    }

    /**
     * gets the argument for the current parsed option. For example,
     * in case of '-d <file>', if current option parsed is 'd' then
     * getOptionArg() would return '<file>'.
     *
     * @return String - argument for current parsed option.
     */
    public String getOptionArg() {
        String retval = null;
        String tmp    = theCurrentOption.getArgument();
        char   c      = theCurrentOption.getArgLetter();
        if (theOptionMatcher.hasArg(c)) {
            retval = tmp;
        }
        return retval;
    }

    private static final String PAD_4_SPC  = dup(' ', 4);
    private static final String PAD_13_SPC = dup(' ', 13);
    private static final String PAD_7_SPC  = dup(' ', 7);

    public void printUsage(String cmdName, Map<String, String> description) {
        StringBuilder builder = new StringBuilder();
        builder.append("Usage: " + cmdName);
        String theOptString = this.theOptionMatcher.getOptString();
        if (theOptString != null && !theOptString.isEmpty()) {
            builder.append(" [options]\n");
            char[] chars = theOptString.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];

                if (i + 1 < chars.length) {
                    if (chars[i + 1] == ':') {
                        // has arg
                        builder.append(PAD_4_SPC).append('-').append(c)
                               .append(" <arg> ");
                        if (description.containsKey("-" + c)) {
                            builder.append(formatUsageDescription(
                                    description.get("-" + c), PAD_13_SPC, 70));
                        }
                        builder.append("\n");
                        i++;
                    } else {
                        builder.append(PAD_4_SPC).append('-').append(c)
                               .append(PAD_7_SPC);
                        if (description.containsKey("-" + c)) {
                            builder.append(formatUsageDescription(
                                    description.get("-" + c), PAD_13_SPC, 70));
                        }
                        builder.append("\n");
                    }
                } else {
                    builder.append(PAD_4_SPC).append('-').append(c)
                           .append(PAD_7_SPC);
                    if (description.containsKey("-" + c)) {
                        builder.append(
                                formatUsageDescription(description.get("-" + c),
                                        PAD_13_SPC, 70));
                    }
                    builder.append("\n");
                }
            }
        }
        builder.append('\n');
        System.out.println(builder);
    }

    public static String dup(char c, int i) {
        if (i <= 0) { return ""; }
        if (i == 1) { return "" + c; }
        StringBuilder b = new StringBuilder();
        for (int j = 0; j < i; j++) {
            b.append(c);
        }
        return b.toString();
    }

    public static String formatUsageDescription(String str, String padding,
                                                int maxLen) {
        int           rowLen = maxLen - padding.length();
        char[]        chars  = str.toCharArray();
        int           length = str.length();
        int           len    = 0;
        StringBuilder b      = new StringBuilder();
//
//        while (len < length) {
//            if (len + rowLen > length) {
//                b.append(chars, len, length - len);
//                len = length;
//                break;
//            } else {
//                b.append(chars, len, rowLen);
//                len = len + rowLen;
//                b.append("\n").append(padding);
//            }
//        }

        for (int i = 0; i < length; i++) {
            char c = chars[i];
            /*
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ： 4E00-9FBF：CJK 统一表意符号
            Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ：F900-FAFF：CJK 兼容象形文字
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ：3400-4DBF：CJK 统一表意符号扩展 A
            CJK的意思是“Chinese，Japanese，Korea”的简写 ，实际上就是指中日韩三国的象形文字的Unicode编码
            Character.UnicodeBlock.GENERAL_PUNCTUATION ：2000-206F：常用标点
            Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ：3000-303F：CJK 符号和标点
            Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ：FF00-FFEF：半角及全角形式
            Character.isLetter(c)：判断字符是否是字母
            Character.isDigit(c)：判断字符是否是数字

             */
            UnicodeBlock ub      = UnicodeBlock.of(c);
            int          charLen = 1;
            if (ub == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || ub == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || ub == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || ub == UnicodeBlock.GENERAL_PUNCTUATION
                    || ub == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                    || ub == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                    // jp
                    || ub == UnicodeBlock.KATAKANA
                    || ub == UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
                    || ub == UnicodeBlock.HIRAGANA) {
                charLen = 2;
            } else if (ub == UnicodeBlock.BASIC_LATIN) {
                charLen = 1;
            }
            b.append(c);
            len += charLen;
            if (len % rowLen == 0) {
                b.append('\n').append(padding);
            }
        }
        return b.toString();
    }

    /**
     * gets list of the commandline arguments. For example, in command
     * such as 'cmd -s -d file file2 file3 file4'  with the usage
     * 'cmd [-s] [-d <file>] <file>...', getCmdArgs() would return
     * the list {file2, file3, file4}.
     *
     * @return String[] - list of command arguments that may appear
     * after options and option arguments.
     */
    public String[] getCmdArgs() {
        String[] retval = new String[theCmdArgs.size()];
        int      i      = 0;
        for (ListIterator it = theCmdArgs.listIterator(); it.hasNext(); ) {
            retval[i++] = (String) it.next();
        }
        return retval;
    }


    private final ListIterator  theOptionsIterator;
    private       Option        theCurrentOption = null;
    private       List          theOptions       = null;
    private       List          theCmdArgs       = null;
    private       OptionMatcher theOptionMatcher = null;

    ///////////////////////////////////////////////////////////
    //
    //   Inner Classes
    //
    ///////////////////////////////////////////////////////////

    // inner class to model an option
    public static class Option {

        private final char   theArgLetter;
        private       String theArgument = null;

        public Option(char argLetter) { theArgLetter = argLetter; }

        public void setArg(String arg) {
            theArgument = arg;
        }

        public boolean hasArg() { return (theArgument != null); }

        public char getArgLetter() { return theArgLetter; }

        public String getArgument() { return theArgument; }
    } // end class Option


    // inner class to query optString for a possible option match,
    // and whether or not a given legal option takes an argument.
    //
    class OptionMatcher {

        public OptionMatcher(String optString) {
            theOptString = optString;
        }


        public boolean match(char c) {
            boolean retval = theOptString.indexOf(c) != -1;
            return retval;
        }

        public boolean hasArg(char c) {
            boolean retval = false;
            int     index  = theOptString.indexOf(c) + 1;
            if (index == theOptString.length()) {
                // reached end of theOptString
                retval = false;
            } else if (theOptString.charAt(index) == ':') {
                retval = true;
            }
            return retval;
        }

        private String theOptString = null;

        public String getOptString() {
            return theOptString;
        }
    } // end class OptionMatcher

    public static void main(String[] args) {
        String[] test1 = {"-d", "d1", "-d", "d2", "-a", "-v", "-s", "a", "b", "c"};
        String[] test2 = {"-havs", "a", "b", "c",};

        String options = "hd:avs:";
        GetOpt getOpt  = new GetOpt(test1, options);
        int    c;
        while ((c = getOpt.getNextOption()) != -1) {
            switch (c) {
                case 'h':
                    System.out.println("Get H");
                    getOpt.printUsage("Test", Collections.emptyMap());
                    System.exit(0);
                    break;
                case 'd':
                    System.out.println("Get D " + getOpt.getOptionArg());
                    break;
                case 'a':
                    System.out.println("Get A");
                    break;
                case 'v':
                    System.out.println("Get V");
                    break;
                case 's':
                    System.out.println("Get S " + getOpt.getOptionArg());
                    break;
            }
        }
        String[] cmdArgs = getOpt.getCmdArgs();
        System.out.println("cmdArgs: " + Arrays.asList(cmdArgs));
        System.out.println("------------------------------");
        getOpt = new GetOpt(test2, options);
        while ((c = getOpt.getNextOption()) != -1) {
            switch (c) {
                case 'h':
                    System.out.println("Get H");
                    getOpt.printUsage("Test", Collections.emptyMap());
                    System.exit(0);
                    break;
                case 'd':
                    System.out.println("Get D " + getOpt.getOptionArg());
                    break;
                case 'a':
                    System.out.println("Get A");
                    break;
                case 'v':
                    System.out.println("Get V");
                    break;
                case 's':
                    System.out.println("Get S " + getOpt.getOptionArg());
                    break;
            }
        }
        cmdArgs = getOpt.getCmdArgs();
        System.out.println("cmdArgs: " + Arrays.asList(cmdArgs));
    }
}// end class GetOpt
