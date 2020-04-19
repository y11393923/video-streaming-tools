package com.sensetime.tsc.streaming.enums;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/14 17:49
 */
public enum CommonCodeEnum implements BaseEnumI{

    SUCCESS("0","成功"),
    SYSTEM_FAILED("10","服务器开小差"),
    REMOTE_SERVICE_UNAVAILABLE("11","远程服务不可用"),
    REMOTE_SERVICE_FAILED("11","远程服务开小差"),
    JSON_PARSE_ERROR("12","JSON解析错误"),
    PARAMETER_VALIDATE_FAILED("100001","参数错误"),
    VIDEO_PATH_ERROR("100002", "视频地址错误"),
    NO_STREAMING_VIDEO("100003", "没有转流的视频"),
    UPLOAD_FILE_CANNOT_BE_EMPTY("100004", "上传文件不能为空"),
    UPLOAD_FILE_TYPE_CANNOT_BE_EMPTY("100005", "上传文件类型不能为空"),
    UPLOAD_FILE_FORMAT_ERROR("100006", "上传文件格式错误"),
    VIDEO_STORAGE_PATH_CREATE_FAILED("100007", "视频存放路径创建失败"),
    NO_VIDEO_IN_COMPRESSED_PACKAGE("100008", "压缩包中没有视频"),
    UNSUPPORTED_FORMAT("100009", "不支持的格式"),
    VIDEO_FORMAT_CONVERSION_ERROR("100010", "视频格式转换错误"),
    VIDEO_ALREADY_EXISTS("100011", "视频已存在"),
    UPLOAD_FILE_TYPE_ERROR("100012", "上传文件类型错误"),
    ;


    CommonCodeEnum(String code,String value){
        this.code=code;
        this.value=value;
    }

    String code;

    String value;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getValue() {
        return value;
    }
}
