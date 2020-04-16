package com.sensetime.tsc.streaming.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/16 9:21
 */
@Data
@Builder
public class VideoStreamingVo {

    private List<VideoStreamInfo> success;

    private List<VideoStreamInfo> failed;

}
