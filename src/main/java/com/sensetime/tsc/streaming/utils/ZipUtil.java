package com.sensetime.tsc.streaming.utils;

import com.google.common.collect.Lists;
import com.sensetime.tsc.streaming.config.InitializeConfiguration;
import com.sensetime.tsc.streaming.response.VideoUploadVo;
import lombok.SneakyThrows;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sensetime.tsc.streaming.constant.CommandConstant.RM_COMMAND;
import static com.sensetime.tsc.streaming.constant.CommandConstant.SH_COMMAND;
import static com.sensetime.tsc.streaming.constant.CommonConstant.*;
import static com.sensetime.tsc.streaming.enums.CommonCodeEnum.UNSUPPORTED_FORMAT;
import static com.sensetime.tsc.streaming.enums.CommonCodeEnum.VIDEO_ALREADY_EXISTS;
import static com.sensetime.tsc.streaming.enums.CommonCodeEnum.VIDEO_NAME_CANNOT_CONTAIN_CHINESE;

@Component
public class ZipUtil {

    private static final Logger logger = LoggerFactory.getLogger(ZipUtil.class);

    @Autowired
    private InitializeConfiguration configuration;

    private Map<String, VideoUploadVo> uploadScheduleMap = new ConcurrentHashMap<>();

    private Map<String, ZipFile> zipFileMap = new ConcurrentHashMap<>();


    /**
     * 压缩文件和文件夹
     *
     * @param srcPathname 需要被压缩的文件或文件夹路径
     * @param zipFilepath 将要生成的zip文件路径
     */
    public void zip(String srcPathname, String zipFilepath) {
        File file = new File(srcPathname);
        if (!file.exists()) {
            throw new RuntimeException("source file or directory " + srcPathname + " does not exist.");
        }

        Project proj = new Project();
        FileSet fileSet = new FileSet();
        fileSet.setProject(proj);
        // 判断是目录还是文件
        if (file.isDirectory()) {
            fileSet.setDir(file);
            // ant中include/exclude规则在此都可以使用
            // 比如:
            // fileSet.setExcludes("**/*.txt");
            // fileSet.setIncludes("**/*.xls");
        } else {
            fileSet.setFile(file);
        }

        Zip zip = new Zip();
        zip.setProject(proj);
        zip.setDestFile(new File(zipFilepath));
        zip.addFileset(fileSet);
        zip.setEncoding(ZIP_ENCODING);
        zip.execute();

        logger.info("compress success.");
    }

    /**
     * 解压缩文件和文件夹
     *
     * @param zipFilepath 需要被解压的zip文件路径
     * @param destDir 将要被解压到哪个文件夹
     */
    public void unzip(String zipFilepath, String destDir) {
        if (!new File(zipFilepath).exists()) {
            throw new RuntimeException("zip file " + zipFilepath + " does not exist.");
        }

        Project project = new Project();
        Expand expand = new Expand();
        expand.setProject(project);
        expand.setTaskType("unzip");
        expand.setTaskName("unzip");
        expand.setEncoding(ZIP_ENCODING);

        expand.setSrc(new File(zipFilepath));
        expand.setDest(new File(destDir));
        expand.execute();

        logger.info("uncompress success.");
    }

    public VideoUploadVo get(String id) throws IOException {
        VideoUploadVo videoUploadVo = uploadScheduleMap.get(id);
        if (Objects.nonNull(videoUploadVo)){
            if ((videoUploadVo.getSuccess() + videoUploadVo.getFailed()) == videoUploadVo.getTotalCount()){
                if (!StringUtils.isEmpty(videoUploadVo.getZipUploadPath())){
                    //解压后删除上传zip文件
                    ShellUtil.exec(SH_COMMAND, String.format(RM_COMMAND, videoUploadVo.getZipUploadPath()));
                    zipFileMap.remove(id).close();
                }
                uploadScheduleMap.remove(id);
            }
        }
        return videoUploadVo;
    }

    public void put(String id, VideoUploadVo videoUploadVo){
        this.uploadScheduleMap.put(id, videoUploadVo);
    }



    public VideoUploadVo unzip(File file, String zipUploadPath, Boolean cover, String id) throws Exception {
        ZipFile zipFile = new ZipFile(file);
        zipFileMap.put(id, zipFile);
        List<VideoUploadVo.UploadFailedVo> failedVos = Collections.synchronizedList(Lists.newArrayList());
        List<ZipEntry> zipEntries = Lists.newArrayList();
        long totalByteSize = 0;
        //遍历zip中所有的文件
        for (Enumeration entries = zipFile.getEntries(); entries.hasMoreElements();){
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            String fileName = zipEntry.getName();
            if (zipEntry.isDirectory() || fileName.indexOf(SYMBOL_POINT) <= 0){
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
            String newFilePath = configuration.getRtspVideoPath() + zipEntry.getName();
            File temp = new File(newFilePath);
            if (temp.exists() && !temp.isDirectory()){
                if (!cover){
                    failedVos.add(VideoUploadVoUtil.buildErrorMessage(fileName, VIDEO_ALREADY_EXISTS.getValue()));
                    continue;
                }
                //如果删除失败则用命令删除
                if (!temp.delete()){
                    ShellUtil.exec(SH_COMMAND, String.format(RM_COMMAND, newFilePath));
                }
            }
            totalByteSize += zipEntry.getSize();
            zipEntries.add(zipEntry);
        }
        AtomicInteger successCount = new AtomicInteger();
        VideoUploadVo videoUploadVo = VideoUploadVo.builder().failedVos(failedVos).failed(failedVos.size())
                .totalByteSize(totalByteSize).totalCount(zipEntries.size()).zipUploadPath(zipUploadPath).build();
        uploadScheduleMap.put(id, videoUploadVo);
        //多线程解压文件
        for (ZipEntry zipEntry : zipEntries) {
            VideoUploadThread thread = new VideoUploadThread(zipFile, zipEntry, configuration.getRtspVideoPath()
                    , failedVos, successCount, id, uploadScheduleMap);
            configuration.getThreadPoolExecutor().execute(thread);
        }
        return videoUploadVo;
    }

    static class VideoUploadThread implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(VideoUploadThread.class);

        private ZipFile zipFile;
        private ZipEntry zipEntry;
        private String rtspVideoPath;
        private List<VideoUploadVo.UploadFailedVo> failedVos;
        private AtomicInteger successCount;
        private String id;
        private Map<String, VideoUploadVo> uploadScheduleMap;

        VideoUploadThread(ZipFile zipFile, ZipEntry zipEntry, String rtspVideoPath, List<VideoUploadVo.UploadFailedVo> failedVos,
                          AtomicInteger successCount, String id , Map<String, VideoUploadVo> uploadScheduleMap) {
            this.zipFile = zipFile;
            this.zipEntry = zipEntry;
            this.rtspVideoPath = rtspVideoPath;
            this.failedVos = failedVos;
            this.successCount = successCount;
            this.id = id;
            this.uploadScheduleMap = uploadScheduleMap;
        }

        @SneakyThrows
        @Override
        public void run() {
            String fileName = zipEntry.getName();
            String newFilePath = rtspVideoPath + zipEntry.getName();
            VideoUploadVo videoUploadVo = uploadScheduleMap.get(id);
            try (InputStream inputStream = zipFile.getInputStream(zipEntry);
                OutputStream outputStream = new FileOutputStream(new File(newFilePath))){
                //将视频解压到指定位置
                byte[] buffer = new byte[BUFFER_SIZE];
                int readBytes;
                int readTotalBytes = 0;
                while((readBytes = inputStream.read(buffer)) > 0){
                    readTotalBytes += readBytes;
                    if (Math.sqrt(readTotalBytes) == 1024){
                        videoUploadVo.getUploadByteSize().getAndAdd(readTotalBytes);
                        uploadScheduleMap.put(id, videoUploadVo);
                        readTotalBytes = 0;
                    }
                    outputStream.write(buffer , 0 , readBytes);
                }
                videoUploadVo.getUploadByteSize().getAndAdd(readTotalBytes);
                videoUploadVo.setSuccess(successCount.incrementAndGet());
            }catch (Exception e){
                logger.error("video upload thread failed ", e);
                failedVos.add(VideoUploadVoUtil.buildErrorMessage(fileName, e.getMessage()));
                videoUploadVo.setFailedVos(failedVos);
                videoUploadVo.setFailed(failedVos.size());
            }finally {
                uploadScheduleMap.put(id, videoUploadVo);
            }
        }
    }


}
