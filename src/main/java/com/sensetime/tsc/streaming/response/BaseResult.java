package com.sensetime.tsc.streaming.response;

import com.sensetime.tsc.streaming.enums.CommonCodeEnum;

import java.io.Serializable;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/14 17:48
 */
public class BaseResult<T> implements Serializable {
    /**
     * 返回异常码
     */
    private String errorCode="0";
    /**
     * 返回异常消息
     */
    private String errorMsg;
    /**
     * 返回具体的业务数据
     */
    private T data;


    public boolean isSuccess(){
        return errorCode.equals(CommonCodeEnum.SUCCESS.getCode());
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "BaseResult{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMsg='" + errorMsg + '\'' +
                ", data=" + data +
                '}';
    }
}
