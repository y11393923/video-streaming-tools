package com.sensetime.tsc.streaming.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
public class VideoUploadVo {
    private Long totalByteSize;

    @Builder.Default
    private AtomicInteger uploadByteSize = new AtomicInteger();

    private Integer totalCount;

    @Builder.Default
    private Integer success = 0;

    @Builder.Default
    private Integer failed = 0;

    private List<UploadFailedVo> failedVos;

    private String zipUploadPath;

    @Data
    @Builder
    public static class UploadFailedVo{

        private String videoName;

        private String message;
    }


}
