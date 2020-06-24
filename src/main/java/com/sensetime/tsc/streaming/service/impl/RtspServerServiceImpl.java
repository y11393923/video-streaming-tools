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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.sensetime.tsc.streaming.constant.CommandConstant.*;
import static com.sensetime.tsc.streaming.constant.CommonConstant.*;

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
    @Value("${ffserver.enabled}")
    private boolean ffserverEnabled;

    @Override
    public VideoStreamingVo videoStreaming() throws Exception {
        long start = System.currentTimeMillis();
        List<VideoStreamInfo> success = Lists.newArrayList();
        List<VideoStreamInfo> failed = Lists.newArrayList();
        if (ffserverEnabled){
            //获取视频目录下所有的文件
            File videoDirectory = new File(configuration.getRtspVideoPath());
            File[] files = videoDirectory.listFiles();
            if (Objects.isNull(files)){
                throw CommonCodeEnum.NO_STREAMING_VIDEO.buildException();
            }
            //存储需要转流的文件名
            List<String> fileNames = Lists.newArrayList();
            for (File file : files) {
                String fileName = file.getName();
                //获取文件名后缀
                String suffix = fileName.substring(fileName.lastIndexOf(SYMBOL_POINT) + 1);
                if (file.isFile() && VIDEO_SUFFIX_FORMAT.contains(suffix)){
                    fileNames.add(fileName);
                }
            }
            //如果没有需要转流的则抛异常
            if (fileNames.isEmpty()){
                throw CommonCodeEnum.NO_STREAMING_VIDEO.buildException();
            }
            //判断该ffserver进程是否在运行
            String execResult = ShellUtil.exec(SH_COMMAND, GET_FFSERVER_PROCESS_CMD);
            if (!StringUtils.isEmpty(execResult)){
                //正在运行则杀掉该进程
                ShellUtil.exec(SH_COMMAND, String.format(KILL_PROCESS_COMMAND, Integer.parseInt(execResult)));
            }
            List<String> newLines = Lists.newArrayList();
            //读取文件
            //todo 大数据会存在内存溢出，有待优化
            File configFile = new File(FFSERVER_CONFIG_PATH);
            List<String> lines = FileUtils.readLines(configFile, CHARSET_UTF8);
            for (String line : lines) {
                if (line.startsWith(STREAM_NAME)){
                    break;
                }
                //拼接新的文件内容
                newLines.add(line);
            }
            newLines.addAll(fileNames.stream().map(fileName -> String.format(STREAM_TEMPLATE, fileName, configuration.getRtspVideoPath() + fileName)).collect(Collectors.toList()));
            //写入文件
            FileUtils.writeLines(configFile, CHARSET_UTF8, newLines);
            //转流
            ShellUtil.exec(SH_COMMAND, String.format(START_FFSERVER_CMD, FFSERVER_PATH, FFSERVER_CONFIG_PATH));
            success.addAll(fileNames.stream().map(e -> VideoStreamInfo.builder().videoName(e).flag(Boolean.TRUE).build()).collect(Collectors.toList()));
        }else{
            //视频格式转换
            List<VideoStreamInfo> videoStreamInfos = videoService.videoFormatConversion(configuration.getRtspVideoPath(), SUFFIX_MP4);
            //判断该rtsp-server端口进程是否在运行
            String execResult = ShellUtil.exec(SH_COMMAND, String.format(GET_RTSP_SERVER_PROCESS_CMD, configuration.getRtspPort()));
            if (!StringUtils.isEmpty(execResult)){
                //正在运行则杀掉该进程
                ShellUtil.exec(SH_COMMAND, String.format(KILL_PROCESS_COMMAND, Integer.parseInt(execResult)));
            }
            //如果没有转流的mp4就直接返回
            execResult = ShellUtil.exec(SH_COMMAND, String.format(FILTER_OUT_MP4_CMD, configuration.getRtspVideoPath(), SUFFIX_MP4));
            if (StringUtils.isEmpty(execResult)){
                throw CommonCodeEnum.NO_STREAMING_VIDEO.buildException();
            }
            //转流
            ShellUtil.exec(SH_COMMAND, String.format(START_RTSP_SERVER_CMD, RTSP_SERVER_PATH, configuration.getRtspPort(), configuration.getMp4VideoPath()));
            //判断是否有格式转换失败的视频
            if (!CollectionUtils.isEmpty(videoStreamInfos)){
                failed.addAll(videoStreamInfos.stream().filter(e -> !e.getFlag()).collect(Collectors.toList()));
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
