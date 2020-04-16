package com.sensetime.tsc.streaming.service;

import com.sensetime.tsc.streaming.response.BaseResult;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/14 16:06
 */
public interface RtspServerService {
    /**
     * 视频转流
     */
    BaseResult videoStreaming();

    /**
     * 视频格式转换
     */
    BaseResult videoFormatConversion(String convertVideoPath, String conversionFormat);
}
