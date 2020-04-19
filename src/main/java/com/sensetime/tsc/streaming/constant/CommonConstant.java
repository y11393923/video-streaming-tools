package com.sensetime.tsc.streaming.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/15 9:27
 */
public class CommonConstant {

    public static final String USER_DIR = System.getProperty("user.dir");

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public static final String SYMBOL_POINT = ".";

    public static final String SYMBOL_BAR = "-";

    public static final String RTSP_ADDRESS = "rtsp://%s:%s/%s";

    public static final String SUFFIX_MP4 = ".mp4";

    public static final String LINE_BREAK = "\n";

    public static final Integer VIDEO_UPLOAD_TYPE_ONE = 1;

    public static final Integer VIDEO_UPLOAD_TYPE_TWO = 2;

    public static final String SUFFIX_ZIP = "zip";

    public static final String ZIP_ENCODING = "GBK";

    public static final Integer BUFFER_SIZE = 1024;

    /**
     * 初始化ffmpeg支持的视频格式
     */
    public static final List<String> VIDEO_SUFFIX_FORMAT = new ArrayList<String>(){{
        this.add("asx");
        this.add("asf");
        this.add("mpg");
        this.add("wmv");
        this.add("3gp");
        this.add("mp4");
        this.add("mov");
        this.add("avi");
        this.add("flv");
    }};

}
