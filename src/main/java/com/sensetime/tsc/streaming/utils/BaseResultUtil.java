package com.sensetime.tsc.streaming.utils;

import com.sensetime.tsc.streaming.enums.CommonCodeEnum;
import com.sensetime.tsc.streaming.response.BaseResult;

import java.util.Optional;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/14 17:52
 */
public class BaseResultUtil {
    public static <T> BaseResult<T> buildBaseResult(T t){
        BaseResult<T> baseResult = new BaseResult<>();
        baseResult.setErrorCode(CommonCodeEnum.SUCCESS.getCode());
        baseResult.setData(t);
        baseResult.setErrorMsg(CommonCodeEnum.SUCCESS.getValue());
        return baseResult;
    }

    /**
     * 创建基础返回对象
     * @return
     */
    public static BaseResult buildEmptyBaseResult(){
        BaseResult baseResult = new BaseResult();
        baseResult.setErrorCode(CommonCodeEnum.SUCCESS.getCode());
        baseResult.setData(Optional.empty());
        baseResult.setErrorMsg(CommonCodeEnum.SUCCESS.getValue());
        return baseResult;
    }

    public static BaseResult buildBaseResult(String message){
        BaseResult baseResult = new BaseResult<>();
        baseResult.setErrorCode(CommonCodeEnum.SYSTEM_FAILED.getCode());
        baseResult.setErrorMsg(message);
        return baseResult;
    }

    public static BaseResult buildBaseResult(CommonCodeEnum codeEnum){
        BaseResult baseResult = new BaseResult<>();
        baseResult.setErrorCode(codeEnum.getCode());
        baseResult.setErrorMsg(codeEnum.getValue());
        return baseResult;
    }


    public static <T> BaseResult<T> buildBaseResultT(CommonCodeEnum codeEnum){
        BaseResult<T> baseResult = new BaseResult<>();
        baseResult.setErrorCode(codeEnum.getCode());
        baseResult.setErrorMsg(codeEnum.getValue());
        return baseResult;
    }

}
