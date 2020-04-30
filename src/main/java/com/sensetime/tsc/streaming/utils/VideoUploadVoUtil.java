package com.sensetime.tsc.streaming.utils;

import com.sensetime.tsc.streaming.response.VideoUploadVo;

import java.util.Collections;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/30 18:05
 */
public class VideoUploadVoUtil {

    public static VideoUploadVo buildSingleError(String fileName, String message){
        return VideoUploadVo.builder()
                .failedVos(Collections.singletonList(
                        VideoUploadVo.UploadFailedVo.builder()
                                .videoName(fileName)
                                .message(message)
                                .build()))
                .failed(1)
                .build();
    }

    public static VideoUploadVo.UploadFailedVo buildErrorMessage(String fileName, String message){
        return VideoUploadVo.UploadFailedVo.builder()
                .videoName(fileName)
                .message(message)
                .build();
    }
}
