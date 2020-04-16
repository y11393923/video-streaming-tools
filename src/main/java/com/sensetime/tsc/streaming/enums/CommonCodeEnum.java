package com.sensetime.tsc.streaming.enums;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/14 17:49
 */
public enum CommonCodeEnum {

    SUCCESS("0","成功"),
    SYSTEM_FAILED("10","服务器开小差"),
    REMOTE_SERVICE_UNAVAILABLE("11","远程服务不可用"),
    REMOTE_SERVICE_FAILED("11","远程服务开小差"),
    JSON_PARSE_ERROR("12","JSON解析错误"),
    PARAMETER_VALIDATE_FAILED("100001","参数错误"),
    VIDEO_PATH_ERROR("100002", "视频地址错误"),
    NO_STREAMING_VIDEO("100003", "没有转流的视频"),
    ;


    CommonCodeEnum(String code,String value){
        this.code=code;
        this.value=value;
    }

    String code;

    String value;

    public String getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
