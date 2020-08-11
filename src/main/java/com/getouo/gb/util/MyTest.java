package com.getouo.gb.util;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MyTest {

	public static void main(String[] args) throws IOException {
		mainStart(0, "/slamtv60.264");
	}
    // 主函数
    public static void mainStart(int port, String fn) throws IOException {
        //String fileName = "F:/test22.264";
//        String fileName = "F:/camera.h264";
//        String fileName = "G:/test.h264";
        String fileName = fn;
        InitSocket(port);
        //ReadFromFile.readFileByBytes(fileName);
        file = new File(fileName);
        in = new FileInputStream(file);

        int seq_num = 0;
        AtomicInteger seqNum = new AtomicInteger(0);
        int bytes = 0;

        // 时间戳增量
        float framerate = 25;
        int timestamp_increse = 0, ts_current = 0;
        timestamp_increse = (int) (90000.0 / framerate); //+0.5);
        // rtp包缓冲
        byte[] sendbuf = new byte[1500];

        while (!(0 == in.available())) {

            NALU_t n = new NALU_t();
            GetAnnexbNALU(n); // 每执行一次, 文件指针指向本次找到的NALU的末尾, 下一位置即为下个NALU的起始码0x000001
            dump(n);//输出NALU的长度和NALU

//            ts_current = ts_current + timestamp_increse;
            n.toRtpPacket(seqNum, ts_current += timestamp_increse).forEach(toSend -> {
                try {
                    Send(toSend, toSend.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


//            // 从文件中 获取一个nalu大小
//            // 判断其大小 分包发送
//            memset(sendbuf, 0, 1500);//情况sendbuf,此时会将上次的时间戳清空,因此需要ts_current来保存上次的时间戳值
//
//            sendbuf[1] = (byte) (sendbuf[1] | 96); // 负载类型号96
//            //System.out.println("-----!"+sendbuf[1]);
//            sendbuf[0] = (byte) (sendbuf[0] | 0x80); // 版本号,此版本固定为2
//            sendbuf[1] = (byte) (sendbuf[1] & 254); //标志位，由具体协议规定其值
//            sendbuf[11] = 10;    //随即指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换
//
//            if (n.len <= 1400) {
//                sendbuf[1] = (byte) (sendbuf[1] | 0x80); // 设置rtp M位为1
//                //sendbug[2], sendbuf[3]赋值seq_num ++ 每发送一次rtp包增1
//                //sendbuf[3] = (byte) seq_num ++
//                System.arraycopy(intToByte(seq_num++), 0, sendbuf, 2, 2);
//                {
//                    // 倒序
//                    byte temp = 0;
//                    temp = sendbuf[3];
//                    sendbuf[3] = sendbuf[2];
//                    sendbuf[2] = temp;
//                }
//
//                // 设置NALU HEADER, 并将这个HEADER填入sendbuf[12]
//                sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.forbidden_bit) << 7);
//                sendbuf[12] = (byte) (sendbuf[12] | ((byte) (n.nal_reference_idc >> 5)) << 5);
//                sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.nal_unit_type));
//
//				System.err.println(n.buf.length);
//				System.err.println(sendbuf.length);
//				System.err.println(n.len);
//                // 同理将sendbuf[13]赋给nalu_payload
//                System.arraycopy(n.buf, 1, sendbuf, 13, n.len - 1);//去掉nalu头的nalu剩余类容写入sendbuf[13]开始的字符串
//
//                ts_current = ts_current + timestamp_increse;
//                //rtp_hdr.timestamp = ts_current;// htonl(ts_current) java默认网络字节序
//                System.arraycopy(intToByte(ts_current), 0, sendbuf, 4, 4);
//                {
//                    // 倒序
//                    byte temp = 0;
//                    temp = sendbuf[4];
//                    sendbuf[4] = sendbuf[7];
//                    sendbuf[7] = temp;
//
//                    temp = sendbuf[5];
//                    sendbuf[5] = sendbuf[6];
//                    sendbuf[6] = temp;
//                }
//                bytes = n.len + 12;                    //获sendbuf的长度,为nalu的长度(包含nalu头但取出起始前缀,加上rtp_header固定长度12个字节)
//                Send(sendbuf, bytes);//发送rtp包
//
//            } else if (n.len > 1400) {
//                // 得到该nalu需要用多少长度为1400字节的rtp包来发送
//                int k = 0, l = 0;
//                k = n.len / 1400; //需要k个1400字节的rtp包
//                l = n.len % 1400; //最后一个rtp包需要装载的字节数
//                int t = 0; // 用于指示当前发送的第几个分片RTP包
//                ts_current = ts_current + timestamp_increse;
//                //rtp_hdr->timestamp=htonl(ts_current);
//                System.arraycopy(intToByte(ts_current), 0, sendbuf, 4, 4);
//                {
//                    // 倒序
//                    byte temp = 0;
//                    temp = sendbuf[4];
//                    sendbuf[4] = sendbuf[7];
//                    sendbuf[7] = temp;
//
//                    temp = sendbuf[5];
//                    sendbuf[5] = sendbuf[6];
//                    sendbuf[6] = temp;
//
//                }
//                while (t <= k) {
//                    //rtp_hdr->seq_no = htons(seq_num ++);//序列号, 每发送一个rtp包增加1
//                    //sendbuf[3] = (byte) seq_num ++;
//                    System.arraycopy(intToByte(seq_num++), 0, sendbuf, 2, 2);
//                    {
//                        // 倒序
//                        byte temp = 0;
//                        temp = sendbuf[3];
//                        sendbuf[3] = sendbuf[2];
//                        sendbuf[2] = temp;
//                    }
//                    if (0 == t) {
//                        // 设置rtp M位
//                        sendbuf[1] = (byte) (sendbuf[1] & 0x7F); // M=0
//                        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
//                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.forbidden_bit) << 7);
//                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) (n.nal_reference_idc >> 5)) << 5);
//                        sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));
//
//                        // 设置FU HEADER,并将这个HEADER填入snedbuf[13]
//                        sendbuf[13] = (byte) (sendbuf[13] & 0xBF);//E=0
//                        sendbuf[13] = (byte) (sendbuf[13] & 0xDF);//R=0
//                        sendbuf[13] = (byte) (sendbuf[13] | 0x80);//S=1
//                        sendbuf[13] = (byte) (sendbuf[13] | ((byte) n.nal_unit_type));
//
//                        // 同理将sendbuf[14]赋给nalu_playload
//                        System.arraycopy(n.buf, 1, sendbuf, 14, 1400);
//                        bytes = 1400 + 14;
//                        Send(sendbuf, bytes);
//                        t++;
//                    }
//                    // 发送一个需要分片的NALU的非第一个分片，清零FU HEADER 的S位，如果该分片是该NALU的最后一个分片，置FU HEADER的E位
//                    else if (k == t) //发送的是最后一个分片，注意最后一个分片的长度可能超过1400字节（当l>1386时）
//                    {
//                        //  设置rtp M位,当前床书的是最后一个分片时该位置1
//                        sendbuf[1] = (byte) (sendbuf[1] | 0x80);
//                        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
//                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.forbidden_bit) << 7);
//                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) (n.nal_reference_idc >> 5)) << 5);
//                        sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));
//
//                        //设置FU HEADER,并将这个HEADER填入sendbuf[13]
//                        sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0
//                        sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0
//                        sendbuf[13] = (byte) (sendbuf[13] | 0x40); //E=1
//                        sendbuf[13] = (byte) (sendbuf[13] | ((byte) n.nal_unit_type));
//
//                        // 将nalu的最后神域的l-1(去掉了一个字节的nalu头)字节类容写入sendbuf[14]开始的字符串
//                        System.arraycopy(n.buf, t * 1400 + 1, sendbuf, 14, l - 1);
//                        bytes = l - 1 + 14;
//                        Send(sendbuf, bytes);
//                        t++;
//                    } else if (t < k && 0 != t) {
//                        //设置rtp M位
//                        sendbuf[1] = (byte) (sendbuf[1] & 0x7F); // M=0
//
//                        // 设置FU INDICATOR,并将这个HEADER填入sendbuf[12]
//                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) n.forbidden_bit) << 7);
//                        sendbuf[12] = (byte) (sendbuf[12] | ((byte) (n.nal_reference_idc >> 5)) << 5);
//                        sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));
//
//                        //设置FU HEADER,并将这个HEADER填入sendbuf[13]
//                        sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0
//                        sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0
//                        sendbuf[13] = (byte) (sendbuf[13] & 0xBF); //E=0
//                        sendbuf[13] = (byte) (sendbuf[13] | ((byte) n.nal_unit_type));
//
//                        System.arraycopy(n.buf, t * 1400 + 1, sendbuf, 14, 1400);//去掉起始前缀的nalu剩余内容写入sendbuf[14]开始的字符串。
//                        bytes = 1400 + 14;                        //获得sendbuf的长度,为nalu的长度（除去原NALU头）加上rtp_header，fu_ind，fu_hdr的固定长度14字节
//                        Send(sendbuf, bytes);//发送rtp包
//                        t++;
//                    }
//                }
//            }
        }
        //free nalu
    }

    public static FileInputStream in = null;

    /**
     * 注释：int到字节数组的转换！
     *
     * @param number
     * @return
     */
    public static byte[] intToByte(int number) {
        int temp = number;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();// 将最低位保存在最低位
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }

	public static class Tmain {
		public static void main(String[] args) {
			ByteBuffer allocate = ByteBuffer.allocate(4);
			allocate.putInt(8);
			System.err.println(Arrays.toString(intToByte(8)));
			System.err.println(Arrays.toString(allocate.array()));
		}
	}

    // 情况buf的值
    public static void memset(byte[] buf, int value, int size) {
        for (int i = 0; i < size; i++) {
            buf[i] = (byte) value;
        }
    }

    public static void dump(NALU_t n) {
        System.out.println("len: " + n.len + " nal_unit_type:" + n.nal_unit_type);

    }

    // 每执行一次，文件的指针指向本次找到的NALU的末尾, 下一个位置即为下个NALU的起始码0x000001
    public static int GetAnnexbNALU(NALU_t nalu) {
        nalu.startcodeprefix_len = 3;// 初始化码流序列的开始符为3个字节
        int pos = 0;
        int StartCodeFound, rewind;

        byte[] tempbytes = new byte[8000000];

        try {
            // 一次读多个字节
            int byteread = 0;

            // 读入多个字节到字节数组中，byteread为一次读入的字节数
            if ((byteread = in.read(tempbytes, 0, 3)) != 3) {
                //System.out.println("读取："+byteread);
                return 0;
            }

            // 判断是否为0x000001
            info2 = FindStartCode2(tempbytes, 0);
            if (info2 != 1) {
                // 如果不是, 再读一个字节
                if ((byteread = in.read(tempbytes, 3, 1)) != 1) {
                    return 0;
                }
                info3 = FindStartCode3(tempbytes, 0);
                if (info3 != 1) {
                    return -1;
                } else {
                    // 如果是0x00000001,得到开始前缀4个字节
                    //pos = 4;
                    nalu.startcodeprefix_len = 4;
                    pos = 4;
                }
            } else {
                // 如果是0x000001,得到开始前缀3个字节
                nalu.startcodeprefix_len = 3;
                pos = 3;
            }

            //查找下一个开始位的标志
            StartCodeFound = 0;
            info2 = 0;
            info3 = 0;


            while (0 == StartCodeFound) {

                if (0 == in.available())//判断是否到文件尾部
                {
                    nalu.len = (pos - 1) - nalu.startcodeprefix_len;
                    System.arraycopy(tempbytes, nalu.startcodeprefix_len, nalu.buf, 0, nalu.len);
                    nalu.forbidden_bit = nalu.buf[0] & 0x80; //1 bit
                    nalu.nal_reference_idc = nalu.buf[0] & 0x60; // 2 bit
                    nalu.nal_unit_type = (nalu.buf[0]) & 0x1f;// 5 bit
                    return pos - 1;
                }

                //读一个字节到tempbytes中
                if ((byteread = in.read(tempbytes, pos++, 1)) != 1) {
                    return 0;
                }
                info3 = FindStartCode3(tempbytes, pos - 4);//判断是否为0x00000001
                if (info3 != 1)
                    info2 = FindStartCode2(tempbytes, pos - 3);//判断是否为0x000001
                if (info2 == 1 || info3 == 1) {
                    StartCodeFound = 1;
                } else {
                    StartCodeFound = 0;
                }
            }


            // Here, we have found another start code (and read length of startcode bytes more than we should
            // have.  Hence, go back in the file
            rewind = (info3 == 1) ? -4 : -3;
            in.skip(rewind);//把文件指针指向前一个NALU的末尾

            // Here the Start code, the complete NALU, and the next start code is in the Buf.
            // The size of Buf is pos, pos+rewind are the number of bytes excluding the next
            // start code, and (pos+rewind)-startcodeprefix_len is the size of the NALU excluding the start code

            nalu.len = (pos + rewind) - nalu.startcodeprefix_len;
            // memcpy (nalu->buf, &Buf[nalu->startcodeprefix_len], nalu->len);//拷贝一个完整的NALU 不拷贝前缀0x000001或0x00000001
            System.arraycopy(tempbytes, nalu.startcodeprefix_len, nalu.buf, 0, nalu.len);
            nalu.forbidden_bit = nalu.buf[0] & 0x80; //1 bit
            nalu.nal_reference_idc = nalu.buf[0] & 0x60; // 2 bit
            nalu.nal_unit_type = (nalu.buf[0]) & 0x1f;// 5 bit

            //free(Buf);

            return (pos + rewind);// 返回两个 开始字符之间隔的字符数, 即包含前缀NALU的长度

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return 0;
    }

    // 判断是否为0x000001,如果是返回1
    public static int FindStartCode2(byte[] Buf, int off) {
        if (Buf[0 + off] != 0 || Buf[1 + off] != 0 || Buf[2 + off] != 1) return 0;
        else return 1;
    }

    // 判断是否为0x00000001,如果是返回1
    public static int FindStartCode3(byte[] Buf, int off) {
        if (Buf[0 + off] != 0 || Buf[1 + off] != 0 || Buf[2 + off] != 0 || Buf[3 + off] != 1) return 0;
        else return 1;
    }


    public static File file = null;

    static int info2 = 0, info3 = 0;

    public static DatagramSocket client = null;
    public static InetAddress addr = null;
    public static int port = 0;

    public static void InitSocket(int p) throws UnknownHostException, SocketException {
        client = new DatagramSocket();
        addr = InetAddress.getByName("localhost");
//        port = 56400;
        port = p;
    }

    // udp发送
    public static void Send(byte[] sendStr, int len) throws IOException {
        //DatagramSocket client = new DatagramSocket();
        //String sendStr = "Hello! I'm Client";
        //byte[] sendBuf;
        //sendBuf = sendStr.getBytes();
        //InetAddress addr = InetAddress.getByName("127.0.0.1");
        //int port = 5050;
        //DatagramPacket sendPacket = new DatagramPacket(sendBuf ,sendBuf.length , addr , port);
        //client.send(sendPacket);
        //System.out.println("Send:"+sendStr);
        //client.close();

        DatagramPacket sendPacket = new DatagramPacket(sendStr, len, addr, port);
        try {
            System.out.println("Send:" + len);
            client.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}

class ReadFromFile {
    /**
     * 以字节为单位读取文件，常用于读二进制文件，如图片、声音、影像等文件。
     */
    public static void readFileByBytes(String fileName) {
        File file = new File(fileName);
        InputStream in = null;

        try {
            System.out.println("以字节为单位读取文件内容，一次读一个字节：");
            // 一次读一个字节
            in = new FileInputStream(file);
            int tempbyte;
            while ((tempbyte = in.read()) != -1) {
                System.out.write(tempbyte);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            System.out.println("以字节为单位读取文件内容，一次读多个字节：");
            // 一次读多个字节
            byte[] tempbytes = new byte[100];
            int byteread = 0;
            in = new FileInputStream(fileName);
            ReadFromFile.showAvailableBytes(in);
            // 读入多个字节到字节数组中，byteread为一次读入的字节数
            while ((byteread = in.read(tempbytes)) != -1) {
                System.out.write(tempbytes, 0, byteread);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    /**
     * 显示输入流中还剩的字节数
     */
    private static void showAvailableBytes(InputStream in) {
        try {
            System.out.println("当前字节输入流中的字节数为:" + in.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

////NALU结构
//public class NALU_t {
//    int startcodeprefix_len;      //! 4 for parameter sets and first slice in picture, 3 for everything else (suggested)
//    int len;                        //! Length of the NAL unit (Excluding the start code, which does not belong to the NALU)
//    int max_size;                //! Nal Unit Buffer size
//    int forbidden_bit;            //! should be always FALSE
//    int nal_reference_idc;        //! NALU_PRIORITY_xxxx
//    int nal_unit_type;            //! NALU_TYPE_xxxx
//    byte[] buf = new byte[8000000];                   //! contains the first byte followed by the EBSP
//    int lost_packets;            //! true, if packet loss is detected
//
//    List<byte[]> toRtpPacket() {
//
//    }
//}


