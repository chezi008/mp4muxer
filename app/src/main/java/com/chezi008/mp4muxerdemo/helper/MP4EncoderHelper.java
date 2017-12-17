package com.chezi008.mp4muxerdemo.helper;

/**
 * 描述：
 * 作者：chezi008 on 2017/9/13 9:07
 * 邮箱：chezi008@163.com
 */

public class MP4EncoderHelper {

    public static native void init(String mp4FilePath, int widht, int height);

    public static native int writeH264Data(byte[] data, int size);

    public static native void close();

    static {
        System.loadLibrary("native-lib");
    }
}
