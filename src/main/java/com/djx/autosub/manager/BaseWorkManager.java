package com.djx.autosub.manager;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.djx.autosub.constant.AudioConstant;
import com.djx.autosub.constant.VideoConstant;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import com.djx.autosub.manager.GetVideo.*;
import com.djx.autosub.springaialibaba.manager.CallChatClientManager;
import com.djx.autosub.util.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Component
@Slf4j
public class BaseWorkManager {

    @Resource
    private BiliGetVideo biliGetVideo;

    @Resource
    private YoutubeGetVideo youtubeGetVideo;

    @Resource
    private UrlGetVideo urlGetVideo;

    @Resource
    private MultipartFileGetVideo multipartFileGetVideo;

    @Resource
    private CallChatClientManager callChatClientManager;

    @Resource
    private AudioSplitManager audioSplitManager;

    @Resource
    private ExecutorService networkRequestThreadPool;

    /**
     * 将用户前端输入的视频下载到服务器本地
     *
     * @param biliUrl    B 站 url
     * @param youtubeUrl youtube url
     * @param videoUrl   视频网址
     * @param videoFile  用户直接上传视频的文件
     * @return 视频本地下载的路径
     */
    public String downloadVideo(String biliUrl, String youtubeUrl, String videoUrl, MultipartFile videoFile) {

        GetVideoInterface getVideoInterface = biliGetVideo;
        Object videoResource = biliUrl;
        byte flag = 0;
        if (biliUrl != null) {
            // 为初始值
            flag++;
        }
        if (youtubeUrl != null) {
            getVideoInterface = youtubeGetVideo;
            videoResource = youtubeUrl;
            flag++;
        }
        if (videoUrl != null) {
            getVideoInterface = urlGetVideo;
            videoResource = videoUrl;
            flag++;
        }
        if (videoFile != null) {
            getVideoInterface = multipartFileGetVideo;
            videoResource = videoFile;
            flag++;
        }
        ThrowUtils.throwIf(flag != 1, ErrorCode.PARAMS_ERROR);
        // 获取视频下载流
        InputStream videoInputStream = getVideoInterface.getVideo(videoResource);
        // 下载视频
        String outputVideoFilePath = VideoConstant.VIDEO_STORAGE_FOLDER_ORIGINAL + IdUtil.simpleUUID() + VideoConstant.VIDEO_FILE_FORMAT;
        File outputVideoFile = new File(outputVideoFilePath);
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(outputVideoFile);
        } catch (FileNotFoundException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(videoInputStream);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bufferedInputStream.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频写入硬盘失败" + e.getMessage());
        }
        return outputVideoFilePath;
    }

    /**
     * 视频转音频
     *
     * @param outputVideoFilePath 视频路径
     * @return 音频路径
     */
    public String videoToAudio(String outputVideoFilePath, String AudioFileFormat) {
        String outputAudioFilePath = AudioConstant.AUDIO_STORAGE_FOLDER + IdUtil.simpleUUID() + AudioFileFormat;
        try {
            VideoToAudioFfmpeg.transform(outputVideoFilePath, outputAudioFilePath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "ffmpeg视频转音频错误");
        }
        return outputAudioFilePath;
    }

    /**
     * 分割视频，多线程调用翻译 API，优化 SRT 并返回（长资源）
     *
     * @param timeBreakpointList       时间分割点列表
     * @param outputAudioFilePath      音频文件路径
     * @param textToSpeechLanguageCode 语言代码
     * @param isTranslate              是否需要翻译
     * @return 优化后的 SRT 字幕文件列表
     */
    public List<String> getOptimizeSrtList(List<Integer> timeBreakpointList, String outputAudioFilePath,
                                           String textToSpeechLanguageCode, boolean isTranslate) {
        List<String> audioSplitFilePathList = audioSplitManager.splitAudio(timeBreakpointList, outputAudioFilePath);
        // 利用 https://www.text-to-speech.cn/ 的产品获取初步按停顿分割的 SRT 字幕文件文本列表
        log.info("开始调用 www.text-to-speech.cn");
        String token = TextToSpeechAudioToText.getToken();
        // 开启异步多线程
        List<File> audioSplitFileList = audioSplitFilePathList.stream()
                .map(File::new)
                .toList();
        AsyncNetworkCallCompletableFuture<File, String> futureAudioToText = new AsyncNetworkCallCompletableFuture<>() {
            @Override
            public AsyncNetworkCallCompletableFuture<File, String>.CallResult asyncTask(File callPara) {
                String result;
                try {
                    result = TextToSpeechAudioToText.audioToText(callPara, textToSpeechLanguageCode, token);
                } catch (Exception e) {
                    return new CallResult(null, e);
                }
                return new CallResult(result, null);
            }
        };
        List<String> preliminarySrtList = futureAudioToText.createAsync(audioSplitFileList, networkRequestThreadPool);
        log.info("调用 www.text-to-speech.cn 完毕");

        // 删除临时音频文件
        for (String audioSplitFilePath : audioSplitFilePathList) {
            DeleteFileUtil.deleteFileIfExists(audioSplitFilePath);
        }

        // 优化并整合 SRT
        return SrtOptimizer.optimizeSrt(preliminarySrtList, textToSpeechLanguageCode, timeBreakpointList, isTranslate);
    }

    /**
     * 调用翻译 API，优化 SRT 并返回（短视频/音频）
     *
     * @param audioFilePath            音频文件路径
     * @param textToSpeechLanguageCode 语种代码
     * @return 优化后的 SRT 字幕文件
     */
    public String getOptimizeSrtList(String audioFilePath, String textToSpeechLanguageCode) {
        // 判断是否为短资源（时长小于 10 分钟）
        int duration = (int) Math.ceil(FfmpegUtil.getAudioDuration(audioFilePath));
        ThrowUtils.throwIf(duration > 600, ErrorCode.PARAMS_ERROR, "资源超过 10 分钟，请选择长视频设置分割点");
        File outputAudioFile = new File(audioFilePath);
        String token = TextToSpeechAudioToText.getToken();
        String preliminarySrt = TextToSpeechAudioToText.audioToText(outputAudioFile, textToSpeechLanguageCode, token);

        // 优化 SRT 字幕文件，使得一句话为一个语义单元，不会在中间截断
        return SrtOptimizer.optimizeSrt(preliminarySrt, textToSpeechLanguageCode);
    }

    /**
     * 调用 AI 翻译，并将结果返回或写入到本地文件（长资源）
     *
     * @param briefExplanation         简要说明
     * @param textToSpeechLanguageCode 翻译前语种
     * @param translationToLangCode    翻译后语种
     * @param optimizeSrtList          优化后的 SRT 字幕文件列表
     * @return 翻译结果
     */
    public String callAiToTranslate(String briefExplanation, String textToSpeechLanguageCode, String translationToLangCode, List<String> optimizeSrtList) {
        log.info("开始 AI 翻译");
        // 开启异步多线程
        AsyncNetworkCallCompletableFuture<String, String> futureCallChatClient = new AsyncNetworkCallCompletableFuture<>() {
            @Override
            public AsyncNetworkCallCompletableFuture<String, String>.CallResult asyncTask(String callPara) {
                if (StrUtil.isBlank(callPara)) {
                    return new CallResult("", null);
                }
                String aiResult;
                try {
                    aiResult = callChatClientManager.callChatClient(callPara, briefExplanation, textToSpeechLanguageCode, translationToLangCode);
                } catch (Exception e) {
                    return new CallResult(null, e);
                }
                return new CallResult(aiResult, null);
            }
        };
        List<String> aiResultList = futureCallChatClient.createAsync(optimizeSrtList, networkRequestThreadPool);
        // 拼接完整 SRT
        log.info("全部片段翻译完毕，开始拼接完整 SRT");
        StringBuilder sb = new StringBuilder();
        for (String aiResult : aiResultList) {
            sb.append(aiResult);
            sb.append("\n\n");
        }
        // 移除末尾多余空行
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}
