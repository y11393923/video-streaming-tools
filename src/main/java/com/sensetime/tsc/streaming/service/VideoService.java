package com.sensetime.tsc.streaming.service;

import com.sensetime.tsc.streaming.response.VideoStreamInfo;
import com.sensetime.tsc.streaming.response.VideoUploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/**
 * @Author: zhouyuyang
 * @Date: 2020/4/17 10:02
 */
public interface VideoService {

    /**
     * 上传视频
     * @param type
     * @param cover
     * @param file
     * @return
     */
    VideoUploadVo upload(Integer type, Boolean cover, MultipartFile file) throws Exception ;

    /**
     * 视频格式转换
     */
    List<VideoStreamInfo> videoFormatConversion(String convertVideoPath, String conversionFormat) throws Exception;

    /**
     * 清理所有视频
     * @throws IOException
     */
    void clearAllVideos() throws IOException;
}
