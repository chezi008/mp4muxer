package com.chezi008.mp4muxerdemo.file;

import android.os.Environment;

import java.io.File;

/**
 * 描述：
 * 作者：chezi008 on 2017/6/29 16:44
 * 邮箱：chezi008@163.com
 */

public class FileConstant {
    public static final String baseFile = Environment.getExternalStorageDirectory().getPath()+"/mp4muxer";
    public static final String h264FileName = "mtv.h264";
    public static final String mp4FileName = "mtv.mp4";
    public static final String aacFileName = "test.aac";

    public static final String mp4FilePath = baseFile + File.separator + mp4FileName;
    public static final String h264FilePath = baseFile + File.separator + h264FileName;
    public static final String aacFilePath = baseFile + File.separator + aacFileName;
}
