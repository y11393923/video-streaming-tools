package com.sensetime.tsc.streaming.service.impl;

import com.sensetime.tsc.streaming.enums.CommonCodeEnum;
import com.sensetime.tsc.streaming.service.VideoUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

import static com.sensetime.tsc.streaming.constant.CommonConstant.*;


/**
 * @Author: zhouyuyang
 * @Date: 2020/4/17 10:02
 */
@Service
public class VideoUploadServiceImpl implements VideoUploadService {

    @Override
    public void upload(Integer type, MultipartFile file) throws Exception {
        checkParam(type, file);

    }


    private void checkParam(Integer type, MultipartFile file){
        if (Objects.isNull(file)){
            throw CommonCodeEnum.UPLOAD_FILE_CANNOT_BE_EMPTY.buildException();
        }
        if (Objects.isNull(type)){
            throw CommonCodeEnum.UPLOAD_FILE_TYPE_CANNOT_BE_EMPTY.buildException();
        }
        String fileName = file.getName();
        String suffix = fileName.substring(fileName.lastIndexOf(SYMBOL_POINT) + 1);
        if (VIDEO_UPLOAD_TYPE_ONE.equals(type) && !VIDEO_SUFFIX_FORMAT.contains(suffix.toLowerCase())){
            throw CommonCodeEnum.UPLOAD_FILE_FORMAT_ERROR.buildException();
        }
        if (VIDEO_UPLOAD_TYPE_TWO.equals(type) && !SUFFIX_ZIP.equals(suffix.toLowerCase())){
            throw CommonCodeEnum.UPLOAD_FILE_FORMAT_ERROR.buildException();
        }
    }
}
