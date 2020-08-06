package com.getouo.gb.util;

import java.util.List;

public class LongBitAccessor {

    public static boolean hasBitIndex(long srcNumber, int bIndex) {
        return hasBit(srcNumber, 1L << bIndex);
    }
    /**
     * 判断对象当前状态是否包含传入的状态值
     * @param srcNumber  源状态集
     * @param number  需要判断的状态值
     * @return 是否存在
     */
    public static boolean hasBit(long srcNumber, long number) {
        return (srcNumber & number) == number;
    }

    /**
     * 判断对象当前状态是否包含传入的状态值
     * @param srcNumber  源状态集
     * @param ns
     * @return 是否存在
     */
    public static boolean hasBits(long srcNumber, long... ns) {
        boolean result = true;
        for (int i = 0; result && i < ns.length; i++) {
            result = hasBit(srcNumber, ns[i]);
        }
        return result;
    }

    /**
     * 判断对象当前状态是否包含传入的状态值
     * @param srcNumber  源状态集
     * @param ns
     * @return
     */
    public static boolean hasBits(long srcNumber, List<Long> ns) {
        boolean result = true;
        for (int i = 0; result && i < ns.size(); i++) {
            result = hasBit(srcNumber, ns.get(i));
        }
        return result;
    }

    /**
     * 判断状态集未拥有某状态
     * @param srcNumber
     * @param n
     * @return
     */
    public static boolean noBit(long srcNumber, long n) {
        return !hasBit(srcNumber, n);
    }

    /**
     * 状态集不拥有被判断的任何一个状态
     * @param srcNumber
     * @param ns
     * @return
     */
    public static boolean noBits(long srcNumber, long...ns) {
        boolean result = true;
        for (int i = 0; result && i < ns.length; i++) {
            result = !hasBit(srcNumber, ns[i]);
        }
        return result;
    }

    /**
     * 状态集不拥有被判断的任何一个状态
     * @param srcNumber
     * @param ns
     * @return
     */
    public static boolean noBits(long srcNumber, List<Long> ns) {
        boolean result = true;
        for (int i = 0; result && i < ns.size(); i++) {
            result = !hasBit(srcNumber, ns.get(i));
        }
        return result;
    }

    public static long changeBit(long srcNumber, long n, boolean isAdd) {
        if (isAdd) return addBit(srcNumber, n);
        else return removeBit(srcNumber, n);
    }
    public static long addBitIndex(long srcNumber, int bitIndex) {
        return addBit(srcNumber, 1L << bitIndex);
    }
    /**
     * 为对象添加状态
     * @param srcNumber  源状态集
     * @param n  需要添加状态值
     * @return 新的状态集
     */
    public static long addBit(long srcNumber, long n) {
        if(hasBit(srcNumber, n)){
            return srcNumber;
        }
        srcNumber = (srcNumber | n);
        return srcNumber;
    }

    /**
     * 批量添加状态值
     * @param ns 需要添加状态值集
     * @return
     */
    public static long addBits(long srcNumber, long... ns) {
        for (int i = 0; i < ns.length; i++) {
            srcNumber = addBit(srcNumber, ns[i]);
        }
        return srcNumber;
    }

    /**
     * 移除对象的某个状态
     * @param n  需要删除状态值
     * @return 新的状态对象
     */
    public static long removeBit(long srcNumber, long n) {
        if(!hasBit(srcNumber, n)){
            return srcNumber;
        }
        srcNumber = (srcNumber ^ n);
        return srcNumber;
    }

    /**
     * 批量移除对象的某些状态
     * @param ns 需要删除的状态值集
     * @return 新的状态对象
     */
    public static long removeBits(long srcNumber, long... ns) {
        for (int i = 0; i < ns.length; i++) {
            srcNumber = removeBit(srcNumber, ns[i]);
        }
        return srcNumber;
    }

    /**
     * 值的二进制表示中有多少个1
     * @return 状态数量
     */
    public static int getBitSize(long srcNumber) {
        int size = 0;
        long valueBuf = srcNumber + 0;
        while (valueBuf > 0) {
            valueBuf = valueBuf & (valueBuf - 1);
            size ++;
        }
        return size;
    }

    public static void main(String[] args) {

        /*
         * 示例:
         * +------------——+------------+
         * |  二进制字面量  | 十进制字面量  |
         * +--------------+------------+
         */

        long src = 0B00000000;

        final long 状态A        = 1 << 0;
        final long 状态B        = 1 << 1;
        final long 状态C        = 1 << 2;

        long stateDemoA = addBit(0L, 状态A);
        long stateDemoB = addBit(stateDemoA, 状态B);
        long stateDemoC = addBit(stateDemoB, 状态C);
        System.err.println();
        System.err.print(hasBit(stateDemoA, 状态A) + "-");
        System.err.print(hasBit(stateDemoA, 状态B) + "-");
        System.err.print(hasBit(stateDemoA, 状态C) + "-");
        System.err.println();
        System.err.print(hasBit(stateDemoB, 状态A) + "-");
        System.err.print(hasBit(stateDemoB, 状态B) + "-");
        System.err.print(hasBit(stateDemoB, 状态C) + "-");
        System.err.println();
        System.err.print(hasBit(stateDemoC, 状态A) + "-");
        System.err.print(hasBit(stateDemoC, 状态B) + "-");
        System.err.print(hasBit(stateDemoC, 状态C) + "-");
        System.err.println();
    }
}
