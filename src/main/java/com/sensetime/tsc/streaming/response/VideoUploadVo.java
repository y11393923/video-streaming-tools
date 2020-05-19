package com.sensetime.tsc.streaming.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VideoUploadVo {
    private Integer totalCount;

    private Integer success;

    private Integer failed;

    private List<UploadFailedVo> failedVos;

    private String zipUploadPath;

    @Data
    @Builder
    public static class UploadFailedVo{

        private String videoName;

        private String message;
    }


}
