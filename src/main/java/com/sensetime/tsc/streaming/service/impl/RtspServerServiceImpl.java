package com.sensetime.tsc.streaming.service.impl;

import com.google.common.collect.Lists;
import com.sensetime.tsc.streaming.enums.CommonCodeEnum;
import com.sensetime.tsc.streaming.response.VideoStreamInfo;
import com.sensetime.tsc.streaming.response.VideoStreamingVo;
import com.sensetime.tsc.streaming.service.RtspServerService;
import com.sensetime.tsc.streaming.service.VideoService;
import com.sensetime.tsc.streaming.utils.IPAddressUtil;
import com.sensetime.tsc.streaming.utils.ShellUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sensetime.tsc.streaming.constant.CommandConstant.*;
import static com.sensetime.tsc.streaming.constant.CommonConstant.*;
import static com.sensetime.tsc.streaming.enums.CommonCodeEnum.UNSUPPORTED_FORMAT;
import static com.sensetime.tsc.streaming.enums.CommonCodeEnum.VIDEO_FORMAT_CONVERSION_ERROR;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/14 16:07
 */
@Service
public class RtspServerServiceImpl implements RtspServerService {
    private static final Logger logger = LoggerFactory.getLogger(RtspServerServiceImpl.class);

    @Value("${rtsp.port:8890}")
    private String rtspPort;
    @Value("${rtsp.video-path:}")
    private String rtspVideoPath;
    private String mp4VideoPath;

    private final Pattern pattern = Pattern.compile("[0-9]*");

    @Autowired
    private VideoService videoService;

    @PostConstruct
    private void init() throws Exception {
        //如果没配视频源地址就用默认的
        if (StringUtils.isEmpty(rtspVideoPath)){
            rtspVideoPath = VIDEO_PATH;
            mp4VideoPath = MP4_VIDEO_PATH;
        }else{
            if (!rtspVideoPath.endsWith(FILE_SEPARATOR)){
                rtspVideoPath += FILE_SEPARATOR;
            }
            mp4VideoPath += SUFFIX_MP4_VIDEO;
        }
        //检查视频存放路径不存在则创建
        File videoParentPath = new File(rtspVideoPath);
        if (!videoParentPath.exists() || !videoParentPath.isDirectory()){
            if (!videoParentPath.mkdirs()){
                throw CommonCodeEnum.VIDEO_STORAGE_PATH_CREATE_FAILED.buildException();
            }
        }
        //初始化rtsp-server脚本权限
        String result = ShellUtil.exec(SH_COMMAND, String.format(QUERY_FILE_PERMISSIONS_CMD, RTSP_SERVER_PATH));
        if (Objects.nonNull(result)){
            if (!pattern.matcher(result).matches()){
                throw new Exception("init rtsp-server error: "+ result);
            }
            if (Integer.parseInt(result.trim()) != FILE_PERMISSIONS){
                ShellUtil.exec(SH_COMMAND, String.format(MODIFY_FILE_PERMISSIONS_CMD, RTSP_SERVER_PATH));
            }
        }
        //初始化ffmpeg脚本权限
        result = ShellUtil.exec(SH_COMMAND, String.format(QUERY_FILE_PERMISSIONS_CMD, FFMPEG_PATH));
        if (Objects.nonNull(result)){
            if (!pattern.matcher(result).matches()){
                throw new Exception("init ffmpeg error: "+ result);
            }
            if (Integer.parseInt(result.trim()) != FILE_PERMISSIONS){
                ShellUtil.exec(SH_COMMAND, String.format(MODIFY_FILE_PERMISSIONS_CMD, FFMPEG_PATH));
            }
        }
    }

    @Override
    public VideoStreamingVo videoStreaming() throws Exception {
        long start = System.currentTimeMillis();
        VideoStreamingVo videoStreamingVo;
        //视频格式转换
        List<VideoStreamInfo> videoStreamInfos = videoService.videoFormatConversion(rtspVideoPath, SUFFIX_MP4);
        //判断该rtsp-server端口进程是否在运行
        String execResult = ShellUtil.exec(SH_COMMAND, String.format(GET_RTSP_SERVER_PROCESS_CMD, rtspPort));
        if (!StringUtils.isEmpty(execResult)){
            //正在运行则杀掉该进程
            String killRtspServerCmd = String.format(KILL_PROCESS_COMMAND, Integer.parseInt(execResult));
            ShellUtil.exec(SH_COMMAND, killRtspServerCmd);
        }
        //转流
        ShellUtil.exec(SH_COMMAND, String.format(START_RTSP_SERVER_CMD, RTSP_SERVER_PATH, rtspPort, mp4VideoPath));
        List<VideoStreamInfo> success;
        List<VideoStreamInfo> failed;
        //判断是否有格式转换的视频
        if (!CollectionUtils.isEmpty(videoStreamInfos)){
            success = videoStreamInfos.stream().filter(VideoStreamInfo::getFlag).collect(Collectors.toList());
            failed = videoStreamInfos.stream().filter(e -> !e.getFlag()).collect(Collectors.toList());
        }else{
            success = Lists.newArrayList();
            failed = Lists.newArrayList();
        }
        //如果没有转流的mp4就直接返回
        execResult = ShellUtil.exec(SH_COMMAND, String.format(FILTER_OUT_MP4_CMD, rtspVideoPath, SUFFIX_MP4));
        videoStreamingVo = VideoStreamingVo.builder().success(success).failed(failed).build();
        if (StringUtils.isEmpty(execResult)){
            throw CommonCodeEnum.NO_STREAMING_VIDEO.buildException();
        }
        //获取所有的MP4文件，默认全部转流成功
        String[] videoNames = execResult.split(LINE_BREAK);
        if (success.size() == 0){
            for (String videoName : videoNames) {
                success.add(VideoStreamInfo.builder().videoName(videoName).flag(Boolean.TRUE).build());
            }
        }else{
            for (String videoName : videoNames) {
                if (success.stream().noneMatch(e -> e.getVideoName().equals(videoName))){
                    success.add(VideoStreamInfo.builder().videoName(videoName).flag(Boolean.TRUE).build());
                }
            }
        }
        //设置rtsp流地址
        String ipAddress = IPAddressUtil.getIpAddress();
        for (VideoStreamInfo videoStreamInfo : success) {
            videoStreamInfo.setRtspAddress(String.format(RTSP_ADDRESS, ipAddress, rtspPort, videoStreamInfo.getVideoName()));
        }
        logger.info("video streaming execution time:{}ms", System.currentTimeMillis() - start);
        return videoStreamingVo;
    }


}
