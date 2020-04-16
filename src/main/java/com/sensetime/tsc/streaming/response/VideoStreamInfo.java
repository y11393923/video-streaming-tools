package com.sensetime.tsc.streaming.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/16 9:45
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStreamInfo {
    /**
     * 视频格式转换命令
     */
    @JSONField(serialize = false)
    private String command;
    /**
     * 视频源名称
     */
    private String videoName;
    /**
     * 视频格式转换是否成功
     */
    @JSONField(serialize = false)
    private Boolean flag;
    /**
     * 错误信息
     */
    private String errorMsg;
    /**
     * 错误详情
     */
    private String detail;
    /**
     * rtsp地址
     */
    private String rtspAddress;
}
