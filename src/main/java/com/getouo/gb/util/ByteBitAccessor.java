package com.getouo.gb.util;

import com.getouo.gb.scl.rtp.RtpHeader;

import java.util.List;

public class ByteBitAccessor {

    public static boolean hasBitIndex(byte srcNumber, int bIndex) {
        return hasBit(srcNumber, (byte) (1 << bIndex));
    }
    /**
     * 判断对象当前状态是否包含传入的状态值
     * @param srcNumber  源状态集
     * @param number  需要判断的状态值
     * @return 是否存在
     */
    public static boolean hasBit(byte srcNumber, byte number) {
        return (srcNumber & number) == number;
    }

    /**
     * 判断对象当前状态是否包含传入的状态值
     * @param srcNumber  源状态集
     * @param ns
     * @return 是否存在
     */
    public static boolean hasBits(byte srcNumber, byte... ns) {
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
    public static boolean hasBits(byte srcNumber, List<Byte> ns) {
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
    public static boolean noBit(byte srcNumber, byte n) {
        return !hasBit(srcNumber, n);
    }

    /**
     * 状态集不拥有被判断的任何一个状态
     * @param srcNumber
     * @param ns
     * @return
     */
    public static boolean noBits(byte srcNumber, byte...ns) {
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
    public static boolean noBits(byte srcNumber, List<Byte> ns) {
        boolean result = true;
        for (int i = 0; result && i < ns.size(); i++) {
            result = !hasBit(srcNumber, ns.get(i));
        }
        return result;
    }

    public static byte changeBit(byte srcNumber, byte n, boolean isAdd) {
        if (isAdd) return addBit(srcNumber, n);
        else return removeBit(srcNumber, n);
    }
    public static byte addBitIndex(byte srcNumber, int bitIndex) {
        return addBit(srcNumber, (byte) (1 << bitIndex));
    }
    /**
     * 为对象添加状态
     * @param srcNumber  源状态集
     * @param n  需要添加状态值
     * @return 新的状态集
     */
    public static byte addBit(byte srcNumber, byte n) {
        if(hasBit(srcNumber, n)){
            return srcNumber;
        }
        srcNumber = (byte) (srcNumber | n);
        return srcNumber;
    }

    /**
     * 批量添加状态值
     * @param ns 需要添加状态值集
     * @return
     */
    public static byte addBits(byte srcNumber, byte... ns) {
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
    public static byte removeBit(byte srcNumber, byte n) {
        if(!hasBit(srcNumber, n)){
            return srcNumber;
        }
        srcNumber = (byte) (srcNumber ^ n);
        return srcNumber;
    }

    /**
     * 批量移除对象的某些状态
     * @param ns 需要删除的状态值集
     * @return 新的状态对象
     */
    public static byte removeBits(byte srcNumber, byte... ns) {
        for (int i = 0; i < ns.length; i++) {
            srcNumber = removeBit(srcNumber, ns[i]);
        }
        return srcNumber;
    }

    /**
     * 值的二进制表示中有多少个1
     * @return 状态数量
     */
    public static int getBitSize(byte srcNumber) {
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

        byte src = 0B00000000;

        final byte 状态A        = 1 << 0;
        final byte 状态B        = 1 << 1;
        final byte 状态C        = 1 << 2;

        byte stateDemoA = addBit((byte) 0, 状态A);
        byte stateDemoB = addBit(stateDemoA, 状态B);
        byte stateDemoC = addBit(stateDemoB, 状态C);
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


        byte byte0 = 0B01100000;
        long lenBuf = 0L;
        if (ByteBitAccessor.hasBit(byte0, RtpHeader.bitByte(7)))
            lenBuf = LongBitAccessor.addBits(lenBuf, 1L << 3);
        if (ByteBitAccessor.hasBit(byte0, RtpHeader.bitByte(6)))
            lenBuf = LongBitAccessor.addBits(lenBuf, 1L << 2);
        if (ByteBitAccessor.hasBit(byte0, RtpHeader.bitByte(5)))
            lenBuf = LongBitAccessor.addBits(lenBuf, 1L << 1);
        if (ByteBitAccessor.hasBit(byte0, RtpHeader.bitByte(4)))
            lenBuf = LongBitAccessor.addBits(lenBuf, 1L);
        System.err.println(lenBuf);
        System.err.println(byte0 >> 4);
        System.err.println(byte0);
        System.err.println(((byte)(byte0 << 8)) >> 8);
        System.err.println((byte0 >> 8) << 8);


        int ten = 0B00010001;
        byte ten2 = 0B00001000;
        System.err.println(ten);
        System.err.println(ten2);
        System.err.println(ten << 4);
        System.err.println(((byte)(ten << 4)) >> 4);
    }
}
