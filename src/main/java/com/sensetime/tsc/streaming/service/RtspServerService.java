package com.sensetime.tsc.streaming.service;

import com.sensetime.tsc.streaming.response.VideoStreamingVo;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/14 16:06
 */
public interface RtspServerService {
    /**
     * 视频转流
     */
    VideoStreamingVo videoStreaming() throws Exception;
}
