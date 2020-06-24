package com.sensetime.tsc.streaming.config;

import com.sensetime.tsc.streaming.enums.CommonCodeEnum;
import com.sensetime.tsc.streaming.service.impl.VideoServiceImpl;
import com.sensetime.tsc.streaming.utils.ShellUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sensetime.tsc.streaming.constant.CommandConstant.*;
import static com.sensetime.tsc.streaming.constant.CommandConstant.FFMPEG_PATH;
import static com.sensetime.tsc.streaming.constant.CommonConstant.*;

@Component
public class InitializeConfiguration {

    @Value("${rtsp.port:3033}")
    private String rtspPort;
    @Value("${rtsp.video-path:}")
    private String rtspVideoPath;
    @Value("${pool.corePoolSize:}")
    private String corePoolSize;
    @Value("${ffserver.enabled}")
    private boolean ffserverEnabled;

    private String mp4VideoPath;

    private ThreadPoolExecutor threadPoolExecutor;

    @PostConstruct
    private void init() throws Exception {
        //设置转流的视频存放路径，如果没配视频源地址就用默认的
        if (StringUtils.isEmpty(rtspVideoPath)){
            rtspVideoPath = VIDEO_PATH;
            mp4VideoPath = MP4_VIDEO_PATH;
        }else{
            if (!rtspVideoPath.endsWith(FILE_SEPARATOR)){
                rtspVideoPath += FILE_SEPARATOR;
            }
            mp4VideoPath = rtspVideoPath + SUFFIX_MP4_VIDEO;
        }
        //检查视频存放路径不存在则创建
        File videoParentPath = new File(rtspVideoPath);
        if (!videoParentPath.exists() || !videoParentPath.isDirectory()){
            if (!videoParentPath.mkdirs()){
                throw CommonCodeEnum.VIDEO_STORAGE_PATH_CREATE_FAILED.buildException();
            }
        }
        //初始化转流服务脚本权限
        String serverPath;
        if (ffserverEnabled){
            serverPath = FFSERVER_PATH;
            //初始化ffserver配置文件权限
            ShellUtil.authorize(FFSERVER_CONFIG_PATH);
            //设置ffserver端口
            File file = new File(FFSERVER_CONFIG_PATH);
            //读取文件
            //todo 大数据会存在内存溢出，有待优化
            List<String> lines = FileUtils.readLines(file, CHARSET_UTF8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(RTSP_PORT_NAME)){
                    //设置新的端口号写入文件
                    lines.set(i, new StringTokenizer(line).nextToken() + " " + rtspPort);
                    FileUtils.writeLines(file, CHARSET_UTF8, lines);
                    break;
                }
            }
        }else{
            serverPath = RTSP_SERVER_PATH;
        }
        ShellUtil.authorize(serverPath);
        //初始化ffmpeg脚本权限
        ShellUtil.authorize(FFMPEG_PATH);
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
