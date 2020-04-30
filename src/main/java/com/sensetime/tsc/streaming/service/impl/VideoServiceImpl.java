package com.sensetime.tsc.streaming.service.impl;

import com.google.common.collect.Lists;
import com.sensetime.tsc.streaming.enums.CommonCodeEnum;
import com.sensetime.tsc.streaming.response.VideoStreamInfo;
import com.sensetime.tsc.streaming.response.VideoUploadVo;
import com.sensetime.tsc.streaming.service.VideoService;
import com.sensetime.tsc.streaming.utils.CheckChineseUtil;
import com.sensetime.tsc.streaming.utils.ShellUtil;
import com.sensetime.tsc.streaming.utils.VideoUploadVoUtil;
import com.sensetime.tsc.streaming.utils.ZipUtil;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
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
    @Value("${rtsp.video-path:}")
    private String rtspVideoPath;
    @Value("${pool.corePoolSize:}")
    private String corePoolSize;

    private ThreadPoolExecutor threadPoolExecutor;


    @PostConstruct
    private void init(){
        if (StringUtils.isEmpty(rtspVideoPath)){
            rtspVideoPath = VIDEO_PATH;
        }else{
            if (!rtspVideoPath.endsWith(FILE_SEPARATOR)){
                rtspVideoPath += FILE_SEPARATOR;
            }
        }
        //初始化线程池
        int coreThreadPoolSize = StringUtils.isEmpty(corePoolSize) ? Runtime.getRuntime().availableProcessors() : Integer.parseInt(corePoolSize);
        threadPoolExecutor = new ThreadPoolExecutor(coreThreadPoolSize, coreThreadPoolSize, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory("exec-command"));
    }

    @Override
    public VideoUploadVo upload(Integer type, Boolean cover, MultipartFile file) throws Exception {
        checkParam(type, file);
        String videoStoragePath = rtspVideoPath;
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
            file.transferTo(newFile);
            return VideoUploadVo.builder()
                    .success(1)
                    .build();
        }else if (VIDEO_UPLOAD_TYPE_TWO.equals(type)){
            String zipUploadPathName = UUID.randomUUID().toString();
            String zipUploadPath = videoStoragePath + zipUploadPathName + FILE_SEPARATOR;
            File zipFilePath = new File(zipUploadPath);
            //创建zip文件存放路径
            if (!zipFilePath.exists() || !zipFilePath.isDirectory()){
                if (!zipFilePath.mkdirs()){
                    throw CommonCodeEnum.VIDEO_STORAGE_PATH_CREATE_FAILED.buildException();
                }
            }
            newFile = new File(zipUploadPath + fileName);
            file.transferTo(newFile);
            ZipFile zipFile = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            List<VideoUploadVo.UploadFailedVo> failedVos = Lists.newArrayList();
            int success = 0;
            try{
                zipFile = new ZipFile(newFile);
                for (Enumeration entries = zipFile.getEntries(); entries.hasMoreElements();){
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    fileName = entry.getName();
                    if (entry.isDirectory() || fileName.indexOf(SYMBOL_POINT) <= 0){
                        continue;
                    }
                    //不能包含中文
                    if (CheckChineseUtil.isContainChinese(fileName)){
                        failedVos.add(VideoUploadVoUtil.buildErrorMessage(fileName, VIDEO_NAME_CANNOT_CONTAIN_CHINESE.getValue()));
                        continue;
                    }
                    //判断视频文件是否支持
                    String videoFileSuffix = fileName.substring(fileName.lastIndexOf(SYMBOL_POINT) + 1);
                    if (!VIDEO_SUFFIX_FORMAT.contains(videoFileSuffix)){
                        failedVos.add(VideoUploadVoUtil.buildErrorMessage(fileName, UNSUPPORTED_FORMAT.getValue()));
                        continue;
                    }
                    String newFilePath = rtspVideoPath + entry.getName();
                    File temp = new File(newFilePath);
                    if (temp.exists() && !temp.isDirectory()){
                        if (!cover){
                            failedVos.add(VideoUploadVoUtil.buildErrorMessage(fileName, VIDEO_ALREADY_EXISTS.getValue()));
                            continue;
                        }
                        //如果删除失败则用命令删除
                        if (!newFile.delete()){
                            ShellUtil.exec(SH_COMMAND, String.format(RM_COMMAND, newFilePath));
                        }
                    }
                    //将视频解压到指定位置
                    inputStream = zipFile.getInputStream(entry);
                    outputStream = new FileOutputStream(temp);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int readBytes;
                    while((readBytes = inputStream.read(buffer)) > 0){
                        outputStream.write(buffer , 0 , readBytes);
                    }
                    success++;
                }
            }finally {
                closeStream(zipFile, outputStream, inputStream);
            }
            ShellUtil.exec(SH_COMMAND, String.format(RM_COMMAND, zipUploadPath));
            return VideoUploadVo.builder()
                    .failedVos(failedVos)
                    .failed(failedVos.size())
                    .success(success)
                    .build();
        }
        return VideoUploadVo.builder().build();
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
                videoStreamInfos.addAll(execCommand(commands));
            }
        }
        return videoStreamInfos;
    }


    /**
     * 多线程执行命令同步返回
     * @param commands
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private List<VideoStreamInfo> execCommand(List<VideoStreamInfo> commands) throws InterruptedException, ExecutionException {
        List<VideoStreamInfo> videoStreamInfos = Lists.newArrayList();
        CountDownLatch countDownLatch = new CountDownLatch(commands.size());
        for (VideoStreamInfo videoStreamInfo : commands) {
            ExecCommandThread execCommandThread = new ExecCommandThread(videoStreamInfo, countDownLatch);
            Future<VideoStreamInfo> future = threadPoolExecutor.submit(execCommandThread);
            videoStreamInfos.add(future.get());
        }
        countDownLatch.await();
        return videoStreamInfos;
    }

    static class ExecCommandThread implements Callable<VideoStreamInfo> {
        private static final Logger logger = LoggerFactory.getLogger(ExecCommandThread.class);

        private VideoStreamInfo videoStreamInfo;
        private CountDownLatch countDownLatch;

        ExecCommandThread(VideoStreamInfo videoStreamInfo, CountDownLatch countDownLatch) {
            this.videoStreamInfo = videoStreamInfo;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public VideoStreamInfo call() throws Exception {
            try {
                ShellUtil.exec(SH_COMMAND, videoStreamInfo.getCommand());
            } catch (Exception e) {
                logger.error("exec command failed", e);
                videoStreamInfo.setFlag(Boolean.FALSE);
                videoStreamInfo.setErrorMsg(VIDEO_FORMAT_CONVERSION_ERROR.getValue());
                videoStreamInfo.setDetail(e.getMessage());
            }
            countDownLatch.countDown();
            return videoStreamInfo;
        }
    }

    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = (namePrefix == null ? "pool-" : namePrefix + "-") + POOL_NUMBER.getAndIncrement();
        }
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + THREAD_NUMBER.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    @Override
    public void clearAllVideos() throws IOException {
        ShellUtil.exec(SH_COMMAND, String.format(RM_COMMAND, rtspVideoPath + SYMBOL_ASTERISK));
    }


    private void closeStream(ZipFile zipFile, OutputStream outputStream, InputStream inputStream) throws IOException {
        if (Objects.nonNull(zipFile)){
            zipFile.close();
        }
        if (Objects.nonNull(outputStream)){
            outputStream.close();
        }
        if (Objects.nonNull(inputStream)){
            inputStream.close();
        }
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
