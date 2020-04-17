package com.sensetime.tsc.streaming.service;

import com.sensetime.tsc.streaming.response.VideoStreamInfo;
import com.sensetime.tsc.streaming.response.VideoStreamingVo;

import java.util.List;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/14 16:06
 */
public interface RtspServerService {
    /**
     * 视频转流
     */
    VideoStreamingVo videoStreaming() throws Exception;

    /**
     * 视频格式转换
     */
    List<VideoStreamInfo> videoFormatConversion(String convertVideoPath, String conversionFormat) throws Exception;
}
