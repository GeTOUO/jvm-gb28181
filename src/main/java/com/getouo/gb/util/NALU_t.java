package com.getouo.gb.util;

import com.getouo.gb.scl.rtp.RtpHeader;
import com.getouo.gb.scl.rtp.RtpHeader$;
import scala.Array;
import scala.collection.ArrayOps;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//NALU结构
public class NALU_t {
    int startcodeprefix_len;      //! 4 for parameter sets and first slice in picture, 3 for everything else (suggested)
    int len;                        //! Length of the NAL unit (Excluding the start code, which does not belong to the NALU), 不包括起始码
    int max_size;                //! Nal Unit Buffer size
    int forbidden_bit;            //! should be always FALSE
    int nal_reference_idc;        //! NALU_PRIORITY_xxxx
    int nal_unit_type;            //! NALU_TYPE_xxxx
    byte[] buf = new byte[8000000];                   //! contains the first byte followed by the EBSP
    int lost_packets;            //! true, if packet loss is detected

    private void writeSeq(byte[] header, int seq) {
        ByteBuffer seqWrite = ByteBuffer.allocateDirect(4);
        seqWrite.putInt(seq);
        seqWrite.position(2);
        seqWrite.get(header, 2, 2);
    }

    List<byte[]> toRtpPacket(AtomicInteger seq, int nextTime) {

        List<byte[]> res = new ArrayList<>();
        byte[] rtpHeaderBytes = new byte[12];

        ByteBuffer timeWrite = ByteBuffer.allocate(4);
        timeWrite.putInt(nextTime);
        timeWrite.position(0);
        timeWrite.get(rtpHeaderBytes, 4, 4);

        rtpHeaderBytes[0] = (byte) (0x80); // 版本号,此版本固定为2
        rtpHeaderBytes[1] = (byte) ((byte) (rtpHeaderBytes[1] | 96) & 254); // 负载类型号96 ; 标志位，由具体协议规定其值
        rtpHeaderBytes[11] = 10; //随即指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换

        if (len <= 1400) {
            rtpHeaderBytes[1] = (byte) (rtpHeaderBytes[1] | 0x80); // 设置rtp M位为1
            writeSeq(rtpHeaderBytes, seq.incrementAndGet());
            byte[] rtpSendBodys = new byte[len + 12];

            rtpSendBodys[12] = (byte) (((byte) forbidden_bit) << 7);
            rtpSendBodys[12] = (byte) (rtpSendBodys[12] | ((byte) (nal_reference_idc >> 5)) << 5);
            rtpSendBodys[12] = (byte) (rtpSendBodys[12] | ((byte) nal_unit_type));
            System.arraycopy(rtpHeaderBytes, 0, rtpSendBodys, 0, 12);//
            System.arraycopy(buf, 1, rtpSendBodys, 13, len - 1);//去掉nalu头的nalu剩余类容写入sendbuf[13]开始的字符串
            res.add(rtpSendBodys);
        } else {

            int l = len % 1400; //最后一个rtp包需要装载的字节数
            int k = len / 1400 + (l == 0 ? 0 : 1); //需要k个1400字节的rtp包

            for (int i = 0; i < k; i++) {
                writeSeq(rtpHeaderBytes, seq.incrementAndGet());
                if (i == 0) {
                    rtpHeaderBytes[1] = (byte) (rtpHeaderBytes[1] & 0x7F); // 设置rtp M位为0

                    byte[] rtpSendBodys = new byte[1400 + 14];

                    // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | ((byte) forbidden_bit) << 7);
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | ((byte) (nal_reference_idc >> 5)) << 5);
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | (byte) (28));

                    // 设置FU HEADER,并将这个HEADER填入snedbuf[13]
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] & 0xBF);//E=0
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] & 0xDF);//R=0
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] | 0x80);//S=1
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] | ((byte) nal_unit_type));

                    System.arraycopy(rtpHeaderBytes, 0, rtpSendBodys, 0, 12);//
                    System.arraycopy(buf, 1 + i * 1400, rtpSendBodys, 14, rtpSendBodys.length - 14);
                    res.add(rtpSendBodys);
                } else if (i == k - 1) {
                    byte[] rtpSendBodys = new byte[(l == 0 ? 1400 : l) + 14];
                    //  设置rtp M位,当前床书的是最后一个分片时该位置1
                    rtpHeaderBytes[1] = (byte) (rtpHeaderBytes[1] | 0x80);

                    // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | ((byte) forbidden_bit) << 7);
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | ((byte) (nal_reference_idc >> 5)) << 5);
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | (byte) (28));

                    //设置FU HEADER,并将这个HEADER填入sendbuf[13]
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] & 0xDF); //R=0
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] & 0x7F); //S=0
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] | 0x40); //E=1
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] | ((byte) nal_unit_type));

                    System.arraycopy(rtpHeaderBytes, 0, rtpSendBodys, 0, 12);//
                    System.arraycopy(buf, 1 + i * 1400, rtpSendBodys, 14, rtpSendBodys.length - 14);
                    res.add(rtpSendBodys);
                } else {
                    byte[] rtpSendBodys = new byte[1400 + 14];

                    rtpHeaderBytes[1] = (byte) (rtpHeaderBytes[1] & 0x7F); // M=0

                    // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | ((byte) forbidden_bit) << 7);
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | ((byte) (nal_reference_idc >> 5)) << 5);
                    rtpSendBodys[12] = (byte) (rtpSendBodys[12] | (byte) (28));

                    //设置FU HEADER,并将这个HEADER填入sendbuf[13]
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] & 0xDF); //R=0
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] & 0x7F); //S=0
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] & 0xBF); //E=0
                    rtpSendBodys[13] = (byte) (rtpSendBodys[13] | ((byte) nal_unit_type));

                    System.arraycopy(rtpHeaderBytes, 0, rtpSendBodys, 0, 12);//
                    System.arraycopy(buf, 1 + i * 1400, rtpSendBodys, 14, rtpSendBodys.length - 14);
                    res.add(rtpSendBodys);
                }
            }
        }
        return res;
    }

    public static void main(String[] args) {


        System.err.println((byte)RtpHeader$.MODULE$.RTP_PAYLOAD_TYPE_H264());

        ByteBuffer allocate = ByteBuffer.allocate(4);
//        System.err.println(Arrays.toString(allocate.array()));
        allocate.putInt(100);
        allocate.position(2);
//        System.err.println(Arrays.toString(allocate.array()));
//        allocate.remaining();
        byte[] bytes = new byte[]{5, 5, 5, 5};
        allocate.get(bytes, 2, 2);
        System.err.println(Arrays.toString(bytes));
        allocate.get(bytes);
        System.err.println(Arrays.toString(bytes));
        System.err.println(Arrays.toString(allocate.array()));


        for (Integer i = 1; i < 65535; i++) {

            short x = i.shortValue();
            if (x <= 0) {
                System.err.println(x);
            }
        }

        Integer s = 1;
    }
}