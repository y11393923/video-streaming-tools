package com.sensetime.tsc.streaming.service.impl;

import com.google.common.collect.Lists;
import com.sensetime.tsc.streaming.config.InitializeConfiguration;
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

    @Autowired
    private InitializeConfiguration configuration;
    @Autowired
    private VideoService videoService;

    @Override
    public VideoStreamingVo videoStreaming() throws Exception {
        long start = System.currentTimeMillis();
        //视频格式转换
        List<VideoStreamInfo> videoStreamInfos = videoService.videoFormatConversion(configuration.getRtspVideoPath(), SUFFIX_MP4);
        //判断该rtsp-server端口进程是否在运行
        String execResult = ShellUtil.exec(SH_COMMAND, String.format(GET_RTSP_SERVER_PROCESS_CMD, configuration.getRtspPort()));
        if (!StringUtils.isEmpty(execResult)){
            //正在运行则杀掉该进程
            String killRtspServerCmd = String.format(KILL_PROCESS_COMMAND, Integer.parseInt(execResult));
            ShellUtil.exec(SH_COMMAND, killRtspServerCmd);
        }
        //转流
        ShellUtil.exec(SH_COMMAND, String.format(START_RTSP_SERVER_CMD, RTSP_SERVER_PATH, configuration.getRtspPort(), configuration.getMp4VideoPath()));
        List<VideoStreamInfo> success = Lists.newArrayList();
        List<VideoStreamInfo> failed;
        //判断是否有格式转换的视频
        if (!CollectionUtils.isEmpty(videoStreamInfos)){
            failed = videoStreamInfos.stream().filter(e -> !e.getFlag()).collect(Collectors.toList());
        }else{
            failed = Lists.newArrayList();
        }
        //如果没有转流的mp4就直接返回
        execResult = ShellUtil.exec(SH_COMMAND, String.format(FILTER_OUT_MP4_CMD, configuration.getRtspVideoPath(), SUFFIX_MP4));
        if (StringUtils.isEmpty(execResult)){
            throw CommonCodeEnum.NO_STREAMING_VIDEO.buildException();
        }
        //获取所有的MP4文件，默认全部转流成功
        String[] videoNames = execResult.split(LINE_BREAK);
        for (String videoName : videoNames) {
            if (videoName.indexOf(SYMBOL_POINT) <= 0){
                continue;
            }
            String fileSuffix = videoName.substring(videoName.lastIndexOf(SYMBOL_POINT));
            if (SUFFIX_MP4.equals(fileSuffix)){
                success.add(VideoStreamInfo.builder().videoName(videoName).flag(Boolean.TRUE).build());
            }
        }
        //设置rtsp流地址
        String ipAddress = IPAddressUtil.getIpAddress();
        for (VideoStreamInfo videoStreamInfo : success) {
            videoStreamInfo.setRtspAddress(String.format(RTSP_ADDRESS, ipAddress, configuration.getRtspPort(), videoStreamInfo.getVideoName()));
        }
        logger.info("video streaming execution time:{}ms", System.currentTimeMillis() - start);
        return VideoStreamingVo.builder().success(success).failed(failed).build();
    }


}
