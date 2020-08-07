package com.getouo.gb.util;

import com.getouo.gb.scl.rtp.RtpPacket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class H264FileReader implements Runnable {

    private final FileInputStream fis;
    private final ArrayBlockingQueue<RtpPacket> packetsQueue;
    final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 32);

    public H264FileReader(String fileName, ArrayBlockingQueue<RtpPacket> packetsQueue) throws FileNotFoundException {
        this.fis = new FileInputStream(fileName);
        this.packetsQueue = packetsQueue;
    }


    @Override
    public void run() {
        while (true) {
            try {
                boolean bufferIsEmpty = false;
                int readLen = -1;
                byte[] buf = new byte[1024 * 8];
                while (this.fis.read(buf) != -1) {
                    if (bufferIsEmpty) {
                        for (int i = 0; i < readLen; i++) {
                            if (i + 3 < readLen && buf[i] == 0 && buf[i+1] == 0 && buf[i+2] == 0 && buf[i+3] == 1) { // start4
                                buffer.put(new byte[]{buf[i], buf[i + 1], buf[i + 2], buf[i + 3]});
                                i += 3;
                                bufferIsEmpty = true;
                            } else if (i + 2 < readLen && buf[i] == 0 && buf[i+1] == 0 && buf[i+2] == 1) {
                                buffer.put(new byte[]{buf[i], buf[i + 1], buf[i + 2]});
                                i += 2;
                                bufferIsEmpty = true;
                            }
                        }
                    } else {

                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
