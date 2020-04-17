package com.sensetime.tsc.streaming.enums;

import com.sensetime.tsc.streaming.exception.BusinessException;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/17 10:15
 */
public interface BaseEnumI {
    String getValue();

    String getCode();

    default BusinessException buildException() {
        return new BusinessException(this.getCode(), this.getValue(), (Throwable)null);
    }

    default BusinessException businessException(Throwable e) {
        return new BusinessException(this.getCode(), this.getValue(), e);
    }
}
