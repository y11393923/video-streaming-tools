package com.sensetime.tsc.streaming.controller;

import com.sensetime.tsc.streaming.response.BaseResult;
import com.sensetime.tsc.streaming.response.VideoUploadVo;
import com.sensetime.tsc.streaming.service.VideoService;
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
@RequestMapping("/video")
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoService videoService;

    /**
     * 上传视频
     * @param type 1为上传视频文件，2为上传压缩包视频
     * @param cover 是否覆盖重名的视频
     * @param file
     * @return
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public BaseResult upload(@RequestParam("type") Integer type,
                             @RequestParam(value = "cover", required = false, defaultValue = "true") Boolean cover,
                             @RequestParam("file") MultipartFile file){
        try {
            return BaseResultUtil.buildBaseResult(videoService.upload(type, cover, file));
        }catch (Exception e){
            logger.error("video upload failed ", e);
            return BaseResultUtil.buildBaseResult(e.getMessage());
        }
    }

    @RequestMapping(value = "/format-conversion", method = RequestMethod.POST)
    public BaseResult videoFormatConversion(String convertVideoPath, String conversionFormat){
        try {
            return BaseResultUtil.buildBaseResult(videoService.videoFormatConversion(convertVideoPath, conversionFormat));
        } catch (Exception e) {
            logger.error("video format conversion execution failed ", e);
            return BaseResultUtil.buildBaseResult(e.getMessage());
        }
    }

    @RequestMapping(value = "/clear", method = RequestMethod.DELETE)
    public BaseResult clear(){
        try {
            videoService.clearAllVideos();
        }catch (Exception e){
            logger.error("video clear failed ", e);
            return BaseResultUtil.buildBaseResult(e.getMessage());
        }
        return BaseResultUtil.buildEmptyBaseResult();
    }
}
