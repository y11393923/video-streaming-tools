package com.sensetime.tsc.streaming.service;

import org.springframework.web.multipart.MultipartFile;


/**
 * @Author: zhouyuyang
 * @Date: 2020/4/17 10:02
 */
public interface VideoUploadService {

    /**
     * 上传视频
     * @param type
     * @param file
     * @return
     */
    void upload(Integer type, MultipartFile file) throws Exception ;
}
