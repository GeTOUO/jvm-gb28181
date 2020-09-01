package com.getouo.gb;

public class MainT {
    static String toNext(String result, String src, char c, int count) {
        if (src == null || src.length() == 0) {
            return result + count + c;
        } else {
            char charAt0 = src.charAt(0);
            if (charAt0 == c) {
                return toNext(result, src.substring(1), c, count + 1);
            } else {
                return toNext(result + count + c, src.substring(1), charAt0, 1);
            }
        }
    }

    static int[] arraySort(int[] arr) {

        int[] res = new int[arr.length];

        for (int i = 0; i < arr.length; i++) {
            res[i] = arr[i];
            for (int j = i; j > 0; j--) {
                if (res[j] < res[j - 1]) {
                    int buf = res[j];
                    res[j] = res[j - 1];
                    res[j - 1] = buf;
                } else {
                    j = 0;
                }
            }
        }
        return res;
    }
    
    public static void main(String[] args) {

    }
}
