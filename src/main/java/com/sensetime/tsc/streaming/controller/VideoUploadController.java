package com.sensetime.tsc.streaming.controller;

import com.sensetime.tsc.streaming.response.BaseResult;
import com.sensetime.tsc.streaming.service.VideoUploadService;
import com.sensetime.tsc.streaming.utils.BaseResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/17 10:00
 */
@RestController
@RequestMapping("/video-upload")
public class VideoUploadController {
    private static final Logger logger = LoggerFactory.getLogger(VideoUploadController.class);

    @Autowired
    private VideoUploadService videoUploadService;

    /**
     * 上传视频
     * @param type 1为上传视频文件，2为上传压缩包视频
     * @param file
     * @return
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public BaseResult upload(@RequestParam("type") Integer type, @RequestParam("file") MultipartFile file){
        try {
            videoUploadService.upload(type, file);
        }catch (Exception e){
            logger.error("video upload failed ", e);
            return BaseResultUtil.buildBaseResult(e.getMessage());
        }
        return BaseResultUtil.buildEmptyBaseResult();
    }
}
