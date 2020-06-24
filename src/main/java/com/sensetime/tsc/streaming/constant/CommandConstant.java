package com.sensetime.tsc.streaming.constant;

import static com.sensetime.tsc.streaming.constant.CommonConstant.*;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/15 9:55
 */
public class CommandConstant {

    public static final String GET_RTSP_SERVER_PROCESS_CMD = "ps -ef | grep rtsp-server | grep -v grep | grep %s |awk '{print $2}'";

    public static final String START_RTSP_SERVER_CMD = "nohup %s -loop=0 -port :%s %s >cmd.log 2>&1 & ";

    private static final String RTSP_SERVER_NAME = "component/rtsp-server";

    private static final String FFSERVER_NAME = "component/ffserver";

    private static final String FFSERVER_CONFIG_NAME = "component/ffserver.cfg";

    public static final String RTSP_SERVER_PATH = USER_DIR + FILE_SEPARATOR + RTSP_SERVER_NAME;

    public static final String FFSERVER_PATH = USER_DIR + FILE_SEPARATOR + FFSERVER_NAME;

    public static final String FFSERVER_CONFIG_PATH = USER_DIR + FILE_SEPARATOR + FFSERVER_CONFIG_NAME;

    private static final String FFMPEG_NAME = "component/ffmpeg";

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

    public static final String MV_COMMAND = "mv %s %s";

    public static final String RM_COMMAND = "rm -rf %s";

    public static final String NEW_LINE = System.getProperty("line.separator");

    public static final String STREAM_TEMPLATE = "<Stream %s>" + NEW_LINE + "File \"%s\"" + NEW_LINE + "Format rtp" + NEW_LINE + "</Stream>";

    public static final String GET_FFSERVER_PROCESS_CMD = "ps -ef | grep ffserver | grep -v grep | grep ffserver.cfg | awk '{print $2}'";

    public static final String START_FFSERVER_CMD = "nohup %s -f %s >cmd.log 2>&1 & ";

}
