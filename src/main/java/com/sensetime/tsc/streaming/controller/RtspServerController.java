package com.sensetime.tsc.streaming.controller;

import com.sensetime.tsc.streaming.response.BaseResult;
import com.sensetime.tsc.streaming.service.RtspServerService;
import com.sensetime.tsc.streaming.utils.BaseResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhouyuyang_vendor
 */
@RestController
@RequestMapping("/rtsp-server")
public class RtspServerController {
    private static final Logger logger = LoggerFactory.getLogger(RtspServerController.class);

    @Autowired
    private RtspServerService rtspServerService;

    @RequestMapping(value = "/diversion", method = RequestMethod.GET)
    public BaseResult diversion(){
        try {
            return BaseResultUtil.buildBaseResult(rtspServerService.videoStreaming());
        } catch (Exception e) {
            logger.error("video streaming execution failed ", e);
            return BaseResultUtil.buildBaseResult(e.getMessage());
        }
    }

}
