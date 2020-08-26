package com.getouo.gb.util;

import java.util.Arrays;

public class BytesPrinter {
    public static final String NEWLINE = System.getProperty("line.separator", "\n");

    private static String[] colIndex = {"00","01","02","03","04","05","06","07", "", "08","09","10","11","12","13","14","15"};

    public static String toStr(final byte[] bytes) {
        if (bytes == null) return "";

        String left = "  offset  |";
        String colFirst = left + Arrays.stream(colIndex).reduce((a, b) -> a + " " + b).orElse("");
        StringBuilder sp = new StringBuilder();
        for (int i = 0; i < colFirst.length(); i++) {
            sp.append("-");
        }
        String spline = sp.toString();

        StringBuilder builder = new StringBuilder(NEWLINE).append(spline).append(NEWLINE).append(colFirst).append(NEWLINE).append(spline);

        for (int i = 0; i < bytes.length; i+=16) {
            builder.append(NEWLINE);
            StringBuilder line = new StringBuilder(rowIndex(i));
            for (int ci = 0; i + ci < bytes.length && ci < 16; ci++) {
                if (ci == 8) line.append(" ");
                String str = Integer.toHexString(bytes[i + ci] & 0xFF);
                if (str.length() == 1) str = "0" + str;
                line.append(str.toUpperCase());
                if (!(i + ci + 1 == bytes.length || ci == 15)) line.append(" ");
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private static String rowIndex(int i) {
        Integer in = (i / 16 + 1) * 16;
        StringBuilder builder = new StringBuilder(in.toString());
        while (builder.length() < 8) {
            builder.insert(0, "0");
        }
        return " " + builder.toString() + " |";
    }


}
