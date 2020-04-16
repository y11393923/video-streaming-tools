package com.sensetime.tsc.streaming.utils;

import org.apache.commons.exec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class ShellUtil {

    private static final Logger logger = LoggerFactory.getLogger(ShellUtil.class);


    public static String exec(String... cmd) throws IOException {
        logger.info("exec command: " + Arrays.toString(cmd));
        CommandLine cmdLine = CommandLine.parse(cmd[0]);
        for(int i = 1; i < cmd.length; i++) {
            if(StringUtils.hasText(cmd[i])) {
                cmdLine.addArgument(cmd[i], false);
            } else {
                cmdLine.addArgument("null", false);
            }
        }

        DefaultExecutor executor = new DefaultExecutor();

        // 防止抛出异常
        executor.setExitValues(null);

        // 命令执行的超时时间
        executor.setWatchdog(new ExecuteWatchdog(10 * 1000));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);

        executor.execute(cmdLine);
        String result = outputStream.toString().trim();
        logger.info("result: " + result);
        return result;
    }

}