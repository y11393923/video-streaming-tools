package com.sensetime.tsc.streaming.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.sensetime.tsc.streaming.config.InitializeConfiguration;
import com.sensetime.tsc.streaming.enums.CommonCodeEnum;
import com.sensetime.tsc.streaming.response.VideoStreamInfo;
import com.sensetime.tsc.streaming.response.VideoUploadVo;
import com.sensetime.tsc.streaming.service.VideoService;
import com.sensetime.tsc.streaming.utils.*;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.sensetime.tsc.streaming.constant.CommandConstant.*;
import static com.sensetime.tsc.streaming.constant.CommonConstant.*;
import static com.sensetime.tsc.streaming.enums.CommonCodeEnum.*;


/**
 * @Author: zhouyuyang
 * @Date: 2020/4/17 10:02
 */
@Service
public class VideoServiceImpl implements VideoService {
    private final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);

    @Autowired
    private ExecCommandUtil execCommandUtil;
    @Autowired
    private ZipUtil zipUtil;
    @Autowired
    private InitializeConfiguration configuration;

    @Override
    public VideoUploadVo upload(Integer type, Boolean cover, MultipartFile file, String id) throws Exception {
        checkParam(type, file);
        String videoStoragePath = configuration.getRtspVideoPath();
        //检查视频上传路径，不存在则创建
        File videoFile = new File(videoStoragePath);
        if (!videoFile.exists() || !videoFile.isDirectory()){
            if (!videoFile.mkdirs()){
                throw CommonCodeEnum.VIDEO_STORAGE_PATH_CREATE_FAILED.buildException();
            }
        }
        String fileName = file.getOriginalFilename();
        File newFile;
        //单视频上传处理
        if (VIDEO_UPLOAD_TYPE_ONE.equals(type)){
            String newFileName = videoStoragePath + fileName;
            //判断名称是否有中文
            if (CheckChineseUtil.isContainChinese(fileName)){
                return VideoUploadVoUtil.buildSingleError(fileName, VIDEO_NAME_CANNOT_CONTAIN_CHINESE.getValue());
            }
            //判断视频名称是否存在 存在则重新封装名称
            newFile = new File(newFileName);
            if (newFile.exists() && !newFile.isDirectory()){
                if (!cover){
                    return VideoUploadVoUtil.buildSingleError(fileName, VIDEO_ALREADY_EXISTS.getValue());
                }
                //如果删除失败则用命令删除
                if (!newFile.delete()){
                    ShellUtil.exec(SH_COMMAND, String.format(RM_COMMAND, newFileName));
                }
            }
            VideoUploadVo uploadVo = VideoUploadVo.builder().totalCount(1).totalByteSize(file.getSize()).build();
            zipUtil.put(id, uploadVo);
            configuration.getThreadPoolExecutor().execute(() -> {
                try (InputStream inputStream = file.getInputStream();
                     OutputStream outputStream = new FileOutputStream(newFile)){
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int readBytes;
                    long readTotalSize = 0;
                    while((readBytes = inputStream.read(buffer)) > 0){
                        readTotalSize += readBytes;
                        if ((readTotalSize / 1024) % 1024 == 0){
                            uploadVo.getUploadByteSize().getAndAdd((int) readTotalSize);
                            zipUtil.put(id, uploadVo);
                        }
                        outputStream.write(buffer , 0 , readBytes);
                    }
                    uploadVo.setSuccess(1);
                    uploadVo.getUploadByteSize().set((int) readTotalSize);
                }catch (Exception e){
                    logger.error("upload video failed", e);
                    uploadVo.setFailed(1);
                    uploadVo.getUploadByteSize().set(uploadVo.getTotalByteSize().intValue());
                }finally {
                    zipUtil.put(id, uploadVo);
                }
            });
            return uploadVo;
        }else if (VIDEO_UPLOAD_TYPE_TWO.equals(type)){
            String zipUploadPath = videoStoragePath + UUID.randomUUID().toString() + FILE_SEPARATOR;
            File zipFilePath = new File(zipUploadPath);
            //创建zip文件存放路径
            if (!zipFilePath.exists() || !zipFilePath.isDirectory()){
                if (!zipFilePath.mkdirs()){
                    throw CommonCodeEnum.VIDEO_STORAGE_PATH_CREATE_FAILED.buildException();
                }
            }
            newFile = new File(zipUploadPath + fileName);
            file.transferTo(newFile);
            return zipUtil.unzip(newFile, zipUploadPath, cover, id);
        }
        return VideoUploadVo.builder().build();
    }

    @Override
    public VideoUploadVo uploadSchedule(String id) throws Exception {
        return zipUtil.get(id);
    }

    @Override
    public List<VideoStreamInfo> videoFormatConversion(String convertVideoPath, String conversionFormat) throws Exception {
        List<VideoStreamInfo> videoStreamInfos = Lists.newArrayList();
        //查询所有不是MP4后缀的文件
        String execResult = ShellUtil.exec(SH_COMMAND, String.format(FILTER_OUT_NOT_MP4_CMD, convertVideoPath, conversionFormat));
        if (!StringUtils.isEmpty(execResult)){
            String[] videos = execResult.split(LINE_BREAK);
            //检查视频存放的路径
            File videoParentPath = new File(convertVideoPath);
            if (!videoParentPath.exists() || !videoParentPath.isDirectory()){
                throw  CommonCodeEnum.VIDEO_PATH_ERROR.buildException();
            }
            //获取路径下所有的文件名称
            String[] list = videoParentPath.list();
            if (Objects.isNull(list) || list.length == 0){
                throw CommonCodeEnum.NO_STREAMING_VIDEO.buildException();
            }
            List<String> videoNames = new ArrayList<>(Arrays.asList(list));
            //去掉文件后缀
            videoNames = videoNames.stream().map(e -> {
                if (e.indexOf(SYMBOL_POINT) > 0){
                    e = e.substring(0, e.lastIndexOf(SYMBOL_POINT));
                }
                return e;
            }).collect(Collectors.toList());
            List<VideoStreamInfo> commands = Lists.newArrayList();
            for (String video : videos) {
                String oldVideoName = video.trim();
                //如果该文件是目录则不处理
                String oldVideoPath = convertVideoPath + oldVideoName;
                File file = new File(oldVideoPath);
                if (!file.exists() || file.isDirectory() || file.getName().indexOf(SYMBOL_POINT) <= 0){
                    continue;
                }
                String fileName = file.getName();
                //判断ffmpeg是否支持该视频格式
                String fileSuffix = fileName.substring(fileName.lastIndexOf(SYMBOL_POINT) + 1);
                if (!VIDEO_SUFFIX_FORMAT.contains(fileSuffix.toLowerCase())){
                    videoStreamInfos.add(VideoStreamInfo.builder()
                            .videoName(fileName)
                            .errorMsg(UNSUPPORTED_FORMAT.getValue())
                            .flag(Boolean.FALSE)
                            .build());
                    continue;
                }
                String oldVideoNamePrefix = oldVideoName.substring(0, oldVideoName.lastIndexOf(SYMBOL_POINT));
                videoNames.remove(oldVideoNamePrefix);
                //判断该视频是否转换过该格式
                if (videoNames.stream().anyMatch(videoName -> videoName.equals(oldVideoNamePrefix))){
                    continue;
                }
                //拼接新的格式文件名称
                fileName = fileName.substring(0, fileName.lastIndexOf(SYMBOL_POINT));
                String newVideoPath = convertVideoPath + fileName + conversionFormat;
                VideoStreamInfo videoStreamInfo = VideoStreamInfo.builder()
                        .videoName(oldVideoName)
                        .command(String.format(FORMAT_CONVERSION_COMMAND, FFMPEG_PATH, oldVideoPath, newVideoPath))
                        .flag(Boolean.TRUE)
                        .build();
                commands.add(videoStreamInfo);
            }
            if (!CollectionUtils.isEmpty(commands)){
                //视频格式转换
                videoStreamInfos.addAll(execCommandUtil.execCommand(commands));
            }
        }
        return videoStreamInfos;
    }

    @Override
    public void clearAllVideos() throws IOException {
        ShellUtil.exec(SH_COMMAND, String.format(RM_COMMAND, configuration.getRtspVideoPath() + SYMBOL_ASTERISK));
    }

    private void checkParam(Integer type, MultipartFile file){
        if (Objects.isNull(file)){
            throw CommonCodeEnum.UPLOAD_FILE_CANNOT_BE_EMPTY.buildException();
        }
        if (Objects.isNull(type)){
            throw CommonCodeEnum.UPLOAD_FILE_TYPE_CANNOT_BE_EMPTY.buildException();
        }
        if (!(VIDEO_UPLOAD_TYPE_ONE.equals(type) || VIDEO_UPLOAD_TYPE_TWO.equals(type))){
            throw CommonCodeEnum.UPLOAD_FILE_TYPE_ERROR.buildException();
        }
        String fileName = file.getOriginalFilename();
        if (StringUtils.isEmpty(fileName) || fileName.indexOf(SYMBOL_POINT) <=0){
            throw CommonCodeEnum.UPLOAD_FILE_FORMAT_ERROR.buildException();
        }
        String suffix = fileName.substring(fileName.lastIndexOf(SYMBOL_POINT) + 1);
        if (VIDEO_UPLOAD_TYPE_ONE.equals(type) && !VIDEO_SUFFIX_FORMAT.contains(suffix.toLowerCase())){
            throw CommonCodeEnum.UPLOAD_FILE_FORMAT_ERROR.buildException();
        }
        if (VIDEO_UPLOAD_TYPE_TWO.equals(type) && !SUFFIX_ZIP.equals(suffix.toLowerCase())){
            throw CommonCodeEnum.UPLOAD_FILE_FORMAT_ERROR.buildException();
        }
    }
}
