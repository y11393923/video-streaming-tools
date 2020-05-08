package com.sensetime.tsc.streaming.utils;

import com.google.common.collect.Lists;
import com.sensetime.tsc.streaming.config.InitializeConfiguration;
import com.sensetime.tsc.streaming.response.VideoStreamInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.sensetime.tsc.streaming.constant.CommandConstant.SH_COMMAND;
import static com.sensetime.tsc.streaming.enums.CommonCodeEnum.VIDEO_FORMAT_CONVERSION_ERROR;

@Component
public class ExecCommandUtil {

    @Autowired
    private InitializeConfiguration configuration;

    /**
     * 多线程执行命令同步返回
     * @param commands
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public List<VideoStreamInfo> execCommand(List<VideoStreamInfo> commands) throws InterruptedException, ExecutionException {
        List<VideoStreamInfo> videoStreamInfos = Lists.newArrayList();
        CountDownLatch countDownLatch = new CountDownLatch(commands.size());
        for (VideoStreamInfo videoStreamInfo : commands) {
            ExecCommandThread execCommandThread = new ExecCommandThread(videoStreamInfo, countDownLatch);
            Future<VideoStreamInfo> future = configuration.getThreadPoolExecutor().submit(execCommandThread);
            videoStreamInfos.add(future.get());
        }
        countDownLatch.await();
        return videoStreamInfos;
    }

    static class ExecCommandThread implements Callable<VideoStreamInfo> {
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
                videoStreamInfo.setErrorMsg(VIDEO_FORMAT_CONVERSION_ERROR.getValue());
                videoStreamInfo.setDetail(e.getMessage());
            }
            countDownLatch.countDown();
            return videoStreamInfo;
        }
    }

}
