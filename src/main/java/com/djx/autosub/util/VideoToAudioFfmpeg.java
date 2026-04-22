package com.djx.autosub.util;

import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Arrays;

@Slf4j
public class VideoToAudioFfmpeg {

    // 要输出的音频格式
    private static final String outputFormat = "mp3";

    /**
     * 获得转化后的文件名
     *
     * @param sourceFilePath : 源视频文件路径
     * @return 转换后的文件名
     */
    public static String getNewFileName(String sourceFilePath) {
        File source = new File(sourceFilePath);
        String fileName = source.getName().substring(0, source.getName().lastIndexOf("."));
        return fileName + "." + outputFormat;
    }

    /**
     * 转化音频格式
     *
     * @param sourceFilePath : 源视频文件路径
     * @param targetFilePath : 目标音乐文件路径
     */
    public static void transform(String sourceFilePath, String targetFilePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", sourceFilePath, targetFilePath);
            pb.redirectErrorStream(true); // 合并标准输出和错误输出
            Process process = pb.start();

            // 读取输出日志
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);
                }
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频转换失败，退出码：" + exitCode);
                }
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取ffmpeg日志时发生错误");
            }
            log.info("转换已完成...");
        } catch (IOException | InterruptedException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频转换失败" + e.getMessage());
        }
    }

    /**
     * 使用 FFmpeg 转换音频并返回输出流（不写入磁盘）
     *
     * @param sourceFilePath 原始音频文件路径
     * @return 转换后的音频输入流
     * @throws IOException 音频转换失败
     * @throws InterruptedException 音频转换失败
     */
    public static InputStream transformToInputStream(String sourceFilePath) throws IOException, InterruptedException {
        // FFmpeg 命令：输出为 pipe:1（标准输出）
        ProcessBuilder pb = new ProcessBuilder("ffmpeg",
                "-i", sourceFilePath,
                "-f", "mp3",       // 输出格式为 MP3，你可以根据需要修改
                "pipe:1");         // 输出到 stdout

        pb.redirectErrorStream(true); // 合并标准错误和标准输出

        Process process = pb.start();

        // 读取 FFmpeg 的日志信息（可选）
//        BufferedReader reader = new BufferedReader(
//                new InputStreamReader(process.getInputStream()));
//        String line;
//        while ((line = reader.readLine()) != null) {
//            log.info(line);
//        }
        log.info("FFmpeg 转码已启动，返回音频输入流...");

        // 返回 FFmpeg 的输出流（转换后的音频数据）
        return process.getInputStream();
    }

    /**
     * 批量转化音频格式
     *
     * @param sourceFolderPath : 源视频文件夹路径
     * @param targetFolderPath : 目标音乐文件夹路径
     */
    public static void batchTransform(String sourceFolderPath, String targetFolderPath) {
        File sourceFolder = new File(sourceFolderPath);
        File targetFolder = new File(targetFolderPath);

        // 如果目标文件夹不存在，则创建它
        if (!targetFolder.exists()) {
            boolean mkdirs = targetFolder.mkdirs();
            ThrowUtils.throwIf(!mkdirs, ErrorCode.SYSTEM_ERROR, "目标文件夹创建失败");
        }

        if (sourceFolder.isDirectory()) {
            File[] files = sourceFolder.listFiles();
            if (files != null && files.length > 0) {
                Arrays.asList(files).forEach(file -> {
                    String sourceFilePath = file.getAbsolutePath();
                    String targetFileName = getNewFileName(sourceFilePath);
                    String targetFilePath = targetFolderPath + File.separator + targetFileName;
                    transform(sourceFilePath, targetFilePath);
                });
            } else {
                log.warn("源文件夹为空，没有文件需要转换。");
            }
        } else {
            log.warn("源路径不是一个有效的文件夹。");
        }
    }
}