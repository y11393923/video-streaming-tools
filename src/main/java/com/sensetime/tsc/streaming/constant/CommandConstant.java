package com.sensetime.tsc.streaming.constant;

import static com.sensetime.tsc.streaming.constant.CommonConstant.FILE_SEPARATOR;
import static com.sensetime.tsc.streaming.constant.CommonConstant.USER_DIR;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/15 9:55
 */
public class CommandConstant {

    public static final String GET_RTSP_SERVER_PROCESS_CMD = "ps -ef | grep rtsp-server | grep -v grep | grep %s |awk '{print $2}'";

    public static final String START_RTSP_SERVER_CMD = "nohup %s -loop=0 -port :%s %s >cmd.log 2>&1 & ";

    private static final String RTSP_SERVER_NAME = "rtsp-server";

    public static final String RTSP_SERVER_PATH = USER_DIR + FILE_SEPARATOR + RTSP_SERVER_NAME;

    private static final String FFMPEG_NAME = "ffmpeg";

    public static final String FFMPEG_PATH = USER_DIR + FILE_SEPARATOR + FFMPEG_NAME;

    public static final String VIDEO_PATH = USER_DIR + FILE_SEPARATOR + "video" + FILE_SEPARATOR;

    public static final String SUFFIX_MP4_VIDEO = "*.mp4";

    public static final String MP4_VIDEO_PATH = VIDEO_PATH + SUFFIX_MP4_VIDEO;

    public static final String SH_COMMAND = "/bin/sh -c ";

    public static final String KILL_PROCESS_COMMAND = "kill -9 %d";

    public static final String FORMAT_CONVERSION_COMMAND = "%s -i %s %s";

    public static final String FILTER_OUT_NOT_MP4_CMD = "ls %s | grep -v '%s'";

    public static final String FILTER_OUT_MP4_CMD = "ls %s | grep '%s'";

    public static final String QUERY_FILE_PERMISSIONS_CMD = "stat -c %%a %s";

    public static final String MODIFY_FILE_PERMISSIONS_CMD = "chmod 777 %s";

    public static final Integer FILE_PERMISSIONS = 777;

}
