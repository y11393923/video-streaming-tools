package com.sensetime.tsc.streaming.controller;

import com.sensetime.tsc.streaming.response.BaseResult;
import com.sensetime.tsc.streaming.service.RtspServerService;
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

    @Autowired
    private RtspServerService rtspServerService;

    @RequestMapping(value = "/video-streaming", method = RequestMethod.GET)
    public BaseResult diversion(){
        return rtspServerService.videoStreaming();
    }

}
