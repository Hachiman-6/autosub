package com.djx.autosub.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.util.FfmpegUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AudioSplitManager {

    private static final int MIN_DURATION = 300;  // 5分钟
    private static final int MAX_DURATION = 900; // 15分钟

    public List<String> splitAudio(List<Integer> timeBreakpointList, String audioFilePath) {
        if (CollectionUtil.isEmpty(timeBreakpointList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分割点列表不能为空");
        }

        // 获取音频总时长（向下取整）
        double totalDuration = FfmpegUtil.getAudioDuration(audioFilePath);
        int totalSeconds = (int) Math.floor(totalDuration);

        // 使列表排序，并检查第一段和中间段
        checkSplitDuration(timeBreakpointList);

        // 检查最后一段
        int lastBreakpoint = timeBreakpointList.get(timeBreakpointList.size() - 1);
        validateSegment(lastBreakpoint, totalSeconds, "最后一段");

        // 开始切割
        File audioFile = FileUtil.file(audioFilePath);
        String outputDir = FileUtil.getCanonicalPath(audioFile.getParentFile());
        String baseName = FileUtil.mainName(audioFile);
        String ext = FileUtil.extName(audioFile);

        List<String> audioSplitFilePathList = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < timeBreakpointList.size(); i++) {
            int end = timeBreakpointList.get(i);
            int duration = end - start;
            String outputFileName = baseName + "_" + (i + 1) + "." + ext;
            String outputPath = FileUtil.file(outputDir, outputFileName).getPath();

            FfmpegUtil.cutAudio(audioFilePath, outputPath, start, duration);
            start = end;
            audioSplitFilePathList.add(outputPath);
        }

        // 最后一段
        double lastDuration = totalDuration - start;
        String lastOutputPath = FileUtil.file(outputDir, baseName + "_" + (timeBreakpointList.size() + 1) + "." + ext).getPath();
        FfmpegUtil.cutAudio(audioFilePath, lastOutputPath, start, lastDuration);
        audioSplitFilePathList.add(lastOutputPath);

        log.info("✅ 音频切割完成，共生成 " + (timeBreakpointList.size() + 1) + " 个文件，保存在: " + outputDir);

        return audioSplitFilePathList;
    }

    /**
     * 使列表排序，并检查第一段和中间段（除最后一段）
     *
     * @param timeBreakpointList 时间分割点列表
     */
    public void checkSplitDuration(List<Integer> timeBreakpointList) {
        // 排序
        timeBreakpointList.sort(Integer::compareTo);

        // 检查第一段：0 -> 第一个分割点
        validateSegment(0, timeBreakpointList.get(0), "第一段");

        // 检查中间段
        for (int i = 1; i < timeBreakpointList.size(); i++) {
            int prev = timeBreakpointList.get(i - 1);
            int curr = timeBreakpointList.get(i);
            validateSegment(prev, curr, "第" + (i + 1) + "段");
        }
    }

    /**
     * 检查一段
     *
     * @param start       开始时间
     * @param end         结束时间
     * @param segmentName 段名称
     */
    private void validateSegment(int start, int end, String segmentName) {
        int duration = end - start;
        if (duration < MIN_DURATION || duration > MAX_DURATION) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("%s（%d秒）超出允许范围 [%d-%d] 秒", segmentName, duration, MIN_DURATION, MAX_DURATION)
            );
        }
    }
}