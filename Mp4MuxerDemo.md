# Android使用Jni mp4v2库将h264裸流合成mp4文件

## 建议使用场景
###一般视频流有如下两种途径获取：
1. Android摄像头采集
2. 服务端传输过来的视频流

如果数据由本机摄像头直接采集，建议使用MediaMuxer类去实现mp4的合成。如果是服务端传输过来的视频流可以使用mp4v2的方法实现mp4的合成。

## 第一步 导入mp4v2的So文件
1. 我demo里面只导入了armeabi 指令集的so包，需要其他类型的可以自己去官网下载进行打包。
2. 将mp4v2的头文件放在libs里面的include文件夹下面，所以再cmake文件里面需要增加`include_directories(libs/include)`命令。
## 使用c++将mp4v2的使用封装了一层
封装了如下几个方法：

	// open or creat a mp4 file.
	MP4FileHandle CreateMP4File(const char *fileName,int width,int height,int timeScale = 90000,int frameRate = 25);
	// wirte 264 metadata in mp4 file.
	bool Write264Metadata(MP4FileHandle hMp4File,LPMP4ENC_Metadata lpMetadata);
	// wirte 264 data, data can contain  multiple frame.
	int WriteH264Data(MP4FileHandle hMp4File,const unsigned char* pData,int size);
	// close mp4 file.
	void CloseMP4File(MP4FileHandle hMp4File);
	// convert H264 file to mp4 file.
	// no need to call CreateMP4File and CloseMP4File,it will create/close mp4 file automaticly.
	bool WriteH264File(const char* pFile264,const char* pFileMp4);

方法说明：

1. CreateMP4File：创建需要写入的mp4文件，可以设置视频的分辨率。默认的时间刻度是90000，帧率是25帧
2. WriteH264Data:写入h264数据，可以写入byte[]类型的数据流，也可是LPMP4ENC_Metadata(sps pps)类型数据。
3. CloseMP4File：转换完成后记得要释放内存，调用次方法
4. WriteH264File：直接将本地h264文件转换成mp4。实现方法是一样的，只是在C++代码里面实现了，将h264数据分割成一帧一帧，再写入至输入文件中。

## Android中如何使用
同样，我在代码中封装了三个本地方法：

	public static native void init(String mp4FilePath, int widht, int height);

    public static native int writeH264Data(byte[] data, int size);

    public static native void close();
从方法名，可以看得出怎么使用，所以在这里就不多赘述了。
## Q&S
1. 录制的视频或快或慢。
