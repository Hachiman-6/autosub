package com.djx.autosub.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.RuntimeUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class FfmpegUtil {

    private static final String FFMPEG_CMD = "ffmpeg";

    /**
     * 使用 FFmpeg 获取音频文件的总时长（秒）
     */
    public static double getAudioDuration(String audioFilePath) {
        // 构建命令
        String[] cmd = {FFMPEG_CMD, "-i", audioFilePath};

        // 执行命令
        Process process = RuntimeUtil.exec(cmd);

        // 读取合并后的输出（stdout + stderr）
        String output = RuntimeUtil.getResult(process, CharsetUtil.systemCharset());

        // 使用正则匹配 Duration: 00:15:23.45
        Pattern pattern = Pattern.compile("Duration:\\s*(\\d{2}):(\\d{2}):(\\d{2}\\.?\\d*)", Pattern.CASE_INSENSITIVE);
        List<String> matches = ReUtil.findAllGroup0(pattern, output);

        if (!matches.isEmpty()) {
            String timeStr = matches.get(0); // 格式如 "00:15:23.45"
            return parseTimeToSeconds(timeStr);
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法从 FFmpeg 输出中提取 Duration。输出内容：" + output);
    }

    /**
     * 切割音频：从指定开始时间，持续指定时长，输出到文件
     *
     * @param inputPath  源音频路径
     * @param outputPath 输出音频路径
     * @param startTime  开始时间（秒）
     * @param duration   持续时间（秒）
     */
    public static void cutAudio(String inputPath, String outputPath, int startTime, int duration) {
        cutAudioMethod(inputPath, outputPath, startTime, String.valueOf(duration));
    }

    /**
     * 切割音频：从指定开始时间，持续指定时长，输出到文件
     *
     * @param inputPath  源音频路径
     * @param outputPath 输出音频路径
     * @param startTime  开始时间（秒）
     * @param duration   持续时间（秒）
     */
    public static void cutAudio(String inputPath, String outputPath, int startTime, double duration) {
        cutAudioMethod(inputPath, outputPath, startTime, String.valueOf(duration));
    }

    /**
     * 切割音频兼容方法：从指定开始时间，持续指定时长，输出到文件
     *
     * @param inputPath  源音频路径
     * @param outputPath 输出音频路径
     * @param startTime  开始时间（秒）
     * @param duration   持续时间（秒）
     */
    private static void cutAudioMethod(String inputPath, String outputPath, int startTime, String duration) {
        String[] cmd = {
                FFMPEG_CMD,
                "-ss", String.valueOf(startTime),
                "-i", inputPath,
                "-t", duration,
                // TODO 请保证切点前后均有大于 1 秒的空白，因为切点会有小于 1 秒的误差
                "-c:a", "copy",
//              "-c:a", "libmp3lame",     // 编码音频（通用）
                "-b:a", "192k",           // 可选比特率
                "-y",                     // 覆盖输出
                outputPath
        };

        Process process = RuntimeUtil.exec(cmd);
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("音频切割被中断", e);
        }

        if (exitCode != 0) {
            String errorOutput = RuntimeUtil.getErrorResult(process, CharsetUtil.systemCharset());
            throw new RuntimeException("FFmpeg 执行失败，退出码: " + exitCode + "，错误信息: " + errorOutput);
        }
    }

    /**
     * 解析 HH:MM:SS 或 HH:MM:SS.mmm 时间格式为秒
     */
    private static double parseTimeToSeconds(String timeStr) {
        int colonIndex = timeStr.indexOf(':');
        String time;
        if (colonIndex != -1) {
            time = timeStr.substring(colonIndex + 2);// 从第一个 ':' 开始截取
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        time = time + "0";
        final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        LocalTime localTime = LocalTime.parse(time, FORMATTER);
        return localTime.toSecondOfDay() + localTime.getNano() / 1_000_000_000.0;
    }
}