package com.chezi008.mp4muxerdemo.file;

import android.util.Log;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 描述：用来将本地h264数据分割成一帧一帧的数据
 * 作者：chezi008 on 2017/6/29 16:50
 * 邮箱：chezi008@163.com
 */

public class H264ReadRunable implements Runnable {
    private static final int READ_BUFFER_SIZE = 1024 * 5;
    private static final int BUFFER_SIZE = 1024 * 1024;

    private String TAG = getClass().getSimpleName();
    private H264ReadListener h264ReadListener;
    private DataInputStream mInputStream;

    public void setH264ReadListener(H264ReadListener h264ReadListener) {
        this.h264ReadListener = h264ReadListener;
    }

    private byte[] buffer;

    @Override
    public void run() {
        try {
            Log.d(TAG, "run: " + FileConstant.h264FilePath);
            mInputStream = new DataInputStream(new FileInputStream(FileConstant.h264FilePath));
            buffer = new byte[BUFFER_SIZE];

            int readLength;
            int naluIndex = 0;
            int offset = 0;
            int bufferLength = 0;

            while ((readLength = mInputStream.read(buffer, offset, READ_BUFFER_SIZE)) > 0) {

                bufferLength += readLength;
                offset = bufferLength;
                for (int i = 5; i < bufferLength - 4; i++) {
                    if (buffer[i] == 0x00 &&
                            buffer[i + 1] == 0x00 &&
                            buffer[i + 2] == 0x00 &&
                            buffer[i + 3] == 0x01) {
                        naluIndex = i;
//                        Log.d(TAG, "run: naluIndex:"+naluIndex);
                        byte[] naluBuffer = new byte[naluIndex];
                        System.arraycopy(buffer,0,naluBuffer,0,naluIndex);
                        h264ReadListener.onFrameData(naluBuffer);
                        offset = bufferLength-naluIndex;
                        bufferLength -=naluIndex;
                        System.arraycopy(buffer,naluIndex,buffer,0,offset);
                        i = 5;
                        Thread.sleep(40);
                    }
                }

            }
            h264ReadListener.onStopRead();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public interface H264ReadListener {
        void onFrameData(byte[] datas);

        void onStopRead();
    }
}
