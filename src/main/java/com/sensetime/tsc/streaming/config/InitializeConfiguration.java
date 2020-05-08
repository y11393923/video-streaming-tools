package com.sensetime.tsc.streaming.config;

import com.sensetime.tsc.streaming.enums.CommonCodeEnum;
import com.sensetime.tsc.streaming.service.impl.VideoServiceImpl;
import com.sensetime.tsc.streaming.utils.ShellUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static com.sensetime.tsc.streaming.constant.CommandConstant.*;
import static com.sensetime.tsc.streaming.constant.CommandConstant.FFMPEG_PATH;
import static com.sensetime.tsc.streaming.constant.CommonConstant.FILE_SEPARATOR;

@Component
public class InitializeConfiguration {

    @Value("${rtsp.port:8890}")
    private String rtspPort;
    @Value("${rtsp.video-path:}")
    private String rtspVideoPath;
    @Value("${pool.corePoolSize:}")
    private String corePoolSize;

    private String mp4VideoPath;

    private ThreadPoolExecutor threadPoolExecutor;

    private final Pattern pattern = Pattern.compile("[0-9]*");

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
        if (StringUtils.isEmpty(rtspVideoPath)){
            rtspVideoPath = VIDEO_PATH;
        }else{
            if (!rtspVideoPath.endsWith(FILE_SEPARATOR)){
                rtspVideoPath += FILE_SEPARATOR;
            }
        }
        //初始化线程池
        int coreThreadPoolSize = StringUtils.isEmpty(corePoolSize) ? Runtime.getRuntime().availableProcessors() : Integer.parseInt(corePoolSize);
        threadPoolExecutor = new ThreadPoolExecutor(coreThreadPoolSize, coreThreadPoolSize, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory("exec-command"));
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

    public String getRtspPort() {
        return rtspPort;
    }

    public String getRtspVideoPath() {
        return rtspVideoPath;
    }

    public String getMp4VideoPath() {
        return mp4VideoPath;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }
}
