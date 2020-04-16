package com.sensetime.tsc.streaming.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.sensetime.tsc.streaming.enums.CommonCodeEnum;
import com.sensetime.tsc.streaming.response.BaseResult;
import com.sensetime.tsc.streaming.response.VideoStreamInfo;
import com.sensetime.tsc.streaming.response.VideoStreamingVo;
import com.sensetime.tsc.streaming.service.RtspServerService;
import com.sensetime.tsc.streaming.utils.BaseResultUtil;
import com.sensetime.tsc.streaming.utils.IPAddressUtil;
import com.sensetime.tsc.streaming.utils.ShellUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static com.sensetime.tsc.streaming.constant.SysyemConstant.*;

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
    @Value("${pool.corePoolSize:0}")
    private Integer corePoolSize;
    private String mp4VideoPath;

    private List<String> videoFormat;

    private ThreadPoolExecutor threadPoolExecutor;

    private final Pattern pattern = Pattern.compile("[0-9]*");

    @PostConstruct
    private void init() throws Exception {
        //初始化ffmpeg支持的视频格式
        videoFormat = Lists.newArrayList();
        videoFormat.add("asx");
        videoFormat.add("asf");
        videoFormat.add("mpg");
        videoFormat.add("wmv");
        videoFormat.add("3gp");
        videoFormat.add("mp4");
        videoFormat.add("mov");
        videoFormat.add("avi");
        videoFormat.add("flv");
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
        //初始化线程池
        if (corePoolSize == 0){
            corePoolSize = Runtime.getRuntime().availableProcessors();
        }
        threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, corePoolSize, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory("exec-command"));
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
    }

    @Override
    public BaseResult videoStreaming(){
        long start = System.currentTimeMillis();
        VideoStreamingVo videoStreamingVo;
        try {
            //视频格式转换
            BaseResult baseResult = videoFormatConversion(rtspVideoPath, SUFFIX_MP4);
            if (Objects.isNull(baseResult) || !CommonCodeEnum.SUCCESS.getCode().equals(baseResult.getErrorCode())){
                return baseResult;
            }
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
            if (Objects.nonNull(baseResult.getData())){
                List<VideoStreamInfo> videoStreamInfos = JSON.parseArray(JSON.toJSONString(baseResult.getData()), VideoStreamInfo.class);
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
                return BaseResultUtil.buildBaseResult(videoStreamingVo);
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
        } catch (Exception e) {
            logger.error("video streaming execution failed ", e);
            return BaseResultUtil.buildBaseResult(e.getMessage());
        }
        logger.info("video streaming execution time:{}ms", System.currentTimeMillis() - start);
        return BaseResultUtil.buildBaseResult(videoStreamingVo);
    }

    @Override
    public BaseResult videoFormatConversion(String convertVideoPath, String conversionFormat) {
        List<VideoStreamInfo> videoStreamInfos = null;
        try {
            //查询所有不是MP4后缀的文件
            String execResult = ShellUtil.exec(SH_COMMAND, String.format(FILTER_OUT_NOT_MP4_CMD, convertVideoPath, conversionFormat));
            if (!StringUtils.isEmpty(execResult)){
                String[] videos = execResult.split(LINE_BREAK);
                //检查视频存放的路径
                File videoParentPath = new File(convertVideoPath);
                if (!videoParentPath.exists() || !videoParentPath.isDirectory()){
                    return BaseResultUtil.buildBaseResult(CommonCodeEnum.VIDEO_PATH_ERROR);
                }
                //获取路径下所有的文件名称
                String[] list = videoParentPath.list();
                if (Objects.isNull(list) || list.length == 0){
                    return BaseResultUtil.buildBaseResult(CommonCodeEnum.NO_STREAMING_VIDEO);
                }
                List<String> videoNames = new ArrayList<>(Arrays.asList(list));
                //去掉文件后缀
                videoNames = videoNames.stream().map(e -> {
                    if (e.indexOf(SYMBOL_POINT) > 0){
                        e = e.substring(0, e.lastIndexOf(SYMBOL_POINT));
                    }
                    return e;
                }).collect(Collectors.toList());
                List<VideoStreamInfo> commands = Lists.newArrayList();
                for (String video : videos) {
                    String oldVideoName = video.trim();
                    //如果该文件是目录则不处理
                    String oldVideoPath = convertVideoPath + oldVideoName;
                    File file = new File(oldVideoPath);
                    if (!file.exists() || file.isDirectory() || file.getName().indexOf(SYMBOL_POINT) <= 0){
                        continue;
                    }
                    StringBuilder fileName = new StringBuilder(file.getName());
                    //判断ffmpeg是否支持该视频格式
                    String fileSuffix = fileName.substring(fileName.lastIndexOf(SYMBOL_POINT) + 1);
                    if (!videoFormat.contains(fileSuffix)){
                        continue;
                    }
                    String oldVideoNamePrefix = oldVideoName.substring(0, oldVideoName.lastIndexOf(SYMBOL_POINT));
                    videoNames.remove(oldVideoNamePrefix);
                    //判断该视频是否转换过该格式
                    if (videoNames.stream().anyMatch(videoName -> videoName.equals(oldVideoNamePrefix))){
                        continue;
                    }
                    //拼接新的格式文件名称
                    fileName = new StringBuilder(fileName.substring(0, fileName.lastIndexOf(SYMBOL_POINT)));
                    String newVideoPath = convertVideoPath + fileName + conversionFormat;
                    VideoStreamInfo videoStreamInfo = VideoStreamInfo.builder()
                            .videoName(oldVideoName)
                            .command(String.format(FORMAT_CONVERSION_COMMAND, oldVideoPath, newVideoPath))
                            .flag(Boolean.TRUE)
                            .build();
                    commands.add(videoStreamInfo);
                }
                if (!CollectionUtils.isEmpty(commands)){
                    //视频格式转换
                    videoStreamInfos = execCommand(commands);
                }
            }
        } catch (Exception e) {
            logger.error("video format conversion execution failed ", e);
            return BaseResultUtil.buildBaseResult(e.getMessage());
        }
        return BaseResultUtil.buildBaseResult(videoStreamInfos);
    }


    /**
     * 多线程执行命令同步返回
     * @param commands
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private List<VideoStreamInfo> execCommand(List<VideoStreamInfo> commands) throws InterruptedException, ExecutionException {
        List<VideoStreamInfo> videoStreamInfos = Lists.newArrayList();
        CountDownLatch countDownLatch = new CountDownLatch(commands.size());
        for (VideoStreamInfo videoStreamInfo : commands) {
            ExecCommandThread execCommandThread = new ExecCommandThread(videoStreamInfo, countDownLatch);
            Future<VideoStreamInfo> future = threadPoolExecutor.submit(execCommandThread);
            videoStreamInfos.add(future.get());
        }
        countDownLatch.await();
        return videoStreamInfos;
    }

    static class ExecCommandThread implements Callable<VideoStreamInfo>{
        private static final Logger logger = LoggerFactory.getLogger(ExecCommandThread.class);

        private VideoStreamInfo videoStreamInfo;
        private CountDownLatch countDownLatch;

        ExecCommandThread(VideoStreamInfo videoStreamInfo, CountDownLatch countDownLatch) {
            this.videoStreamInfo = videoStreamInfo;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public VideoStreamInfo call() throws Exception {
            try {
                ShellUtil.exec(SH_COMMAND, videoStreamInfo.getCommand());
            } catch (Exception e) {
                logger.error("exec command failed", e);
                videoStreamInfo.setFlag(Boolean.FALSE);
                videoStreamInfo.setErrorMsg("video format conversion error");
                videoStreamInfo.setDetail(e.getMessage());
            }
            countDownLatch.countDown();
            return videoStreamInfo;
        }
    }

    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = (namePrefix == null ? "pool-" : namePrefix + "-") + POOL_NUMBER.getAndIncrement();
        }
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + THREAD_NUMBER.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }


}
