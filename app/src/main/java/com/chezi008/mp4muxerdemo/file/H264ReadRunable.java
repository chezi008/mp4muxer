package com.chezi008.mp4muxerdemo.file;

import android.util.Log;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 描述：用来将本地h264数据分割成一帧一帧的数据
 * 作者：chezi008 on 2017/6/29 16:50
 * 邮箱：chezi008@163.com
 */

public class H264ReadRunable implements Runnable {
    String TAG = getClass().getSimpleName();
    private H264ReadListener h264ReadListener;
    private boolean mStopFlag;
    private DataInputStream mInputStream;
    long startMs = System.currentTimeMillis();
    long timeoutUs = 10000;
    byte[] marker0 = new byte[]{0, 0, 0, 1};
    byte[] dummyFrame = new byte[]{0x00, 0x00, 0x01, 0x20};
    byte[] streamBuffer = new byte[1024 * 1024 * 5];

    public void setH264ReadListener(H264ReadListener h264ReadListener) {
        this.h264ReadListener = h264ReadListener;
    }

    @Override
    public void run() {
        try {
            Log.d(TAG, "run: " + FileConstant.h264FilePath);
            mInputStream = new DataInputStream(new FileInputStream(FileConstant.h264FilePath));
            while (true) {
                int length = mInputStream.available();
                if (length > 0) {
                    int count = mInputStream.read(streamBuffer);
                    mStopFlag = false;
                    int bytes_cnt = 0;
                    while (mStopFlag == false) {
                        bytes_cnt = streamBuffer.length;
                        if (bytes_cnt == 0) {
                            streamBuffer = dummyFrame;
                        }

                        int startIndex = 0;
                        int remaining = bytes_cnt;
                        while (true) {
                            if (remaining == 0 || startIndex >= remaining) {
                                break;
                            }
                            int nextFrameStart = KMPMatch(marker0, streamBuffer, startIndex + 2, remaining);
                            if (nextFrameStart == -1) {
                                nextFrameStart = remaining;
                            } else {
                            }
                            byte[] frameData = new byte[nextFrameStart - startIndex];
                            System.arraycopy(streamBuffer, startIndex, frameData, 0, frameData.length);
                            if (h264ReadListener == null) {
                                throw new NullPointerException("readFle callback is null");
                            }
                            h264ReadListener.onFrameData(frameData);
                            startIndex = nextFrameStart;
                        }
                        mStopFlag = true;
                        h264ReadListener.onStopRead();
                        Log.d("haha", "decodeLoop: end");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    int KMPMatch(byte[] pattern, byte[] bytes, int start, int remain) {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int[] lsp = computeLspTable(pattern);

        int j = 0;  // Number of chars matched in pattern
        for (int i = start; i < remain; i++) {
            while (j > 0 && bytes[i] != pattern[j]) {
                // Fall back in the pattern
                j = lsp[j - 1];  // Strictly decreasing
            }
            if (bytes[i] == pattern[j]) {
                // Next char matched, increment position
                j++;
                if (j == pattern.length)
                    return i - (j - 1);
            }
        }

        return -1;  // Not found
    }

    int[] computeLspTable(byte[] pattern) {
        int[] lsp = new int[pattern.length];
        lsp[0] = 0;  // Base case
        for (int i = 1; i < pattern.length; i++) {
            // Start by assuming we're extending the previous LSP
            int j = lsp[i - 1];
            while (j > 0 && pattern[i] != pattern[j])
                j = lsp[j - 1];
            if (pattern[i] == pattern[j])
                j++;
            lsp[i] = j;
        }
        return lsp;
    }

    public interface H264ReadListener {
        void onFrameData(byte[] datas);

        void onStopRead();
    }
}
