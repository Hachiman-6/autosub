package com.djx.autosub.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.djx.autosub.constant.AudioConstant;
import com.djx.autosub.constant.SubtitlesConstant;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import com.djx.autosub.manager.AudioSplitManager;
import com.djx.autosub.manager.BaseWorkManager;
import com.djx.autosub.model.dto.GetAiSrtFromVideoRequest;
import com.djx.autosub.model.dto.GetAudioFromVideoRequest;
import com.djx.autosub.model.dto.GetSrtFromVideoRequest;
import com.djx.autosub.model.enums.TextToSpeechWebLanguageEnum;
import com.djx.autosub.service.FromVideoService;
import com.djx.autosub.springaialibaba.manager.CallChatClientManager;
import com.djx.autosub.util.DeleteFileUtil;
import com.djx.autosub.util.ReturnFileDownloadUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
public class FromVideoServiceImpl implements FromVideoService {

    @Resource
    private BaseWorkManager baseWorkManager;

    @Resource
    private CallChatClientManager callChatClientManager;

    @Resource
    private AudioSplitManager audioSplitManager;

    /**
     * 视频转音频并给客户端返回音频下载流
     *
     * @param getAudioFromVideoRequest 请求参数
     * @param response                 响应
     */
    @Override
    public void getAudioFromVideo(GetAudioFromVideoRequest getAudioFromVideoRequest, HttpServletResponse response) {

        String biliUrl = getAudioFromVideoRequest.getBiliUrl();
        String youtubeUrl = getAudioFromVideoRequest.getYoutubeUrl();
        String videoUrl = getAudioFromVideoRequest.getVideoUrl();
        MultipartFile videoFile = getAudioFromVideoRequest.getVideoFile();

        // 下载视频并获得视频路径
        String outputVideoFilePath = baseWorkManager.downloadVideo(biliUrl, youtubeUrl, videoUrl, videoFile);

        // 调用 ffmpeg 视频转音频，提供用户下载的用 mp3 格式
        String outputAudioFilePath = baseWorkManager.videoToAudio(outputVideoFilePath, AudioConstant.AUDIO_FILE_FORMAT_MP3);

        ReturnFileDownloadUtil.downloadFile(outputAudioFilePath, response);

        // 删除本机临时文件
        DeleteFileUtil.deleteFileIfExists(outputVideoFilePath);
        DeleteFileUtil.deleteFileIfExists(outputAudioFilePath);
    }

    /**
     * 解析视频无需翻译返回 SRT 文件下载流
     *
     * @param getSrtFromVideoRequest 请求参数
     * @param response               响应
     */
    @Override
    public void getSrtFromVideo(GetSrtFromVideoRequest getSrtFromVideoRequest, HttpServletResponse response) {
        String biliUrl = getSrtFromVideoRequest.getBiliUrl();
        String youtubeUrl = getSrtFromVideoRequest.getYoutubeUrl();
        String videoUrl = getSrtFromVideoRequest.getVideoUrl();
        MultipartFile videoFile = getSrtFromVideoRequest.getVideoFile();
        String textToSpeechLanguageCode = getSrtFromVideoRequest.getTextToSpeechLanguageCode();
        List<Integer> timeBreakpointList = getSrtFromVideoRequest.getTimeBreakpointList();

        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(textToSpeechLanguageCode), ErrorCode.PARAMS_ERROR, "源文件语种代码为空");
        boolean containsCode = TextToSpeechWebLanguageEnum.containsCode(textToSpeechLanguageCode);
        ThrowUtils.throwIf(!containsCode, ErrorCode.PARAMS_ERROR, "语言代码枚举错误");
        if (!CollUtil.isEmpty(timeBreakpointList)) {
            audioSplitManager.checkSplitDuration(timeBreakpointList);
        }

        // 下载视频并获得视频路径
        String outputVideoFilePath = baseWorkManager.downloadVideo(biliUrl, youtubeUrl, videoUrl, videoFile);

        // 调用 ffmpeg 视频转音频，内部处理 MAV
        String outputAudioFilePath = baseWorkManager.videoToAudio(outputVideoFilePath, AudioConstant.AUDIO_FILE_FORMAT_WAV);

        String optimizeSrt;
        // 分割视频，多线程调用翻译 API，优化 SRT 并返回（长资源）
        // 对于时长较长的音频（需大于 10 分钟），即 timeBreakpointList 不为空，需进行切割，防止网址访问超时被卡住
        if (CollectionUtil.isNotEmpty(timeBreakpointList)) {
            // 对于时长较长的音频（需大于 10 分钟），即 timeBreakpointList 不为空，需进行切割，防止网址访问超时被卡住
            List<String> optimizeSrtList = baseWorkManager.getOptimizeSrtList(timeBreakpointList, outputAudioFilePath,
                    textToSpeechLanguageCode, false);
            optimizeSrt = optimizeSrtList.get(0);
            ThrowUtils.throwIf(optimizeSrtList.size() != 1 || StrUtil.isBlank(optimizeSrt),
                    ErrorCode.SYSTEM_ERROR, "生成字幕错误，或返回字幕为空");
        } else {
            // 判断是否为短资源（时长小于 10 分钟），调用翻译 API，优化 SRT 并返回（短资源）
            optimizeSrt = baseWorkManager.getOptimizeSrtList(outputAudioFilePath, textToSpeechLanguageCode);
            ThrowUtils.throwIf(StrUtil.isBlank(optimizeSrt), ErrorCode.SYSTEM_ERROR, "返回字幕为空");
        }

        // 删除本机临时文件
        DeleteFileUtil.deleteFileIfExists(outputVideoFilePath);
        DeleteFileUtil.deleteFileIfExists(outputAudioFilePath);

        // 返回文件下载流
        ReturnFileDownloadUtil.downloadTextContent(optimizeSrt,
                IdUtil.simpleUUID() + SubtitlesConstant.SUBTITLES_FILE_FORMAT, response);
    }

    /**
     * 解析视频并获得翻译后的 SRT
     *
     * @param getAiSrtFromVideoRequest 请求参数
     * @param response                 响应
     */
    @Override
    public void getTranslateSrtFromVideo(@RequestBody GetAiSrtFromVideoRequest getAiSrtFromVideoRequest,
                                         HttpServletResponse response) {
        String biliUrl = getAiSrtFromVideoRequest.getBiliUrl();
        String youtubeUrl = getAiSrtFromVideoRequest.getYoutubeUrl();
        String videoUrl = getAiSrtFromVideoRequest.getVideoUrl();
        MultipartFile videoFile = getAiSrtFromVideoRequest.getVideoFile();
        String textToSpeechLanguageCode = getAiSrtFromVideoRequest.getTextToSpeechLanguageCode();
        List<Integer> timeBreakpointList = getAiSrtFromVideoRequest.getTimeBreakpointList();
        String briefExplanation = getAiSrtFromVideoRequest.getBriefExplanation();
        String translationToLangCode = getAiSrtFromVideoRequest.getTranslationToLangCode();

        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(textToSpeechLanguageCode), ErrorCode.PARAMS_ERROR, "源文件语种代码为空");
        boolean containsCode = TextToSpeechWebLanguageEnum.containsCode(textToSpeechLanguageCode);
        ThrowUtils.throwIf(!containsCode, ErrorCode.PARAMS_ERROR, "语言代码枚举错误");
        if (!CollUtil.isEmpty(timeBreakpointList)) {
            audioSplitManager.checkSplitDuration(timeBreakpointList);
        }
        ThrowUtils.throwIf(StrUtil.isBlank(translationToLangCode), ErrorCode.PARAMS_ERROR, "翻译后语种代码为空");
        containsCode = TextToSpeechWebLanguageEnum.containsCode(translationToLangCode);
        ThrowUtils.throwIf(!containsCode, ErrorCode.PARAMS_ERROR, "语言代码枚举错误");

        // 下载视频并获得视频路径
        String outputVideoFilePath = baseWorkManager.downloadVideo(biliUrl, youtubeUrl, videoUrl, videoFile);

        // 调用 ffmpeg 视频转音频，用 wav 格式调用 SRT API 更快
        String outputAudioFilePath = baseWorkManager.videoToAudio(outputVideoFilePath, AudioConstant.AUDIO_FILE_FORMAT_WAV);

        // 如果 timeBreakpointList 不为空，则需分割，防止网址访问超时被卡住
        String srtTranslate;
        if (CollectionUtil.isNotEmpty(timeBreakpointList)) {
            // 对于时长较长的音频（需大于 10 分钟），即 timeBreakpointList 不为空，需进行切割，防止网址访问超时被卡住
            // 获得利于 AI 翻译每段大小合适的 SRT 文本列表
            List<String> optimizeSrtList = baseWorkManager.getOptimizeSrtList(timeBreakpointList, outputAudioFilePath,
                    textToSpeechLanguageCode, true);
            // 给 AI 提供 SRT 翻译前的语种 翻译后的语种 简述一下讲了什么内容
            ThrowUtils.throwIf(CollUtil.isEmpty(optimizeSrtList), ErrorCode.SYSTEM_ERROR, "AI 翻译内容为空");

            // 调用 AI 翻译 SRT 字幕文件，生成译文 SRT 字幕文件
            srtTranslate = baseWorkManager.callAiToTranslate(briefExplanation, textToSpeechLanguageCode,
                    translationToLangCode, optimizeSrtList);

        } else {
            // 判断是否为短资源（时长小于 10 分钟），调用翻译 API，优化 SRT 并返回（短资源）
            String optimizeSrt = baseWorkManager.getOptimizeSrtList(outputAudioFilePath, textToSpeechLanguageCode);
            // 调用 AI 翻译 SRT 字幕文件，生成译文 SRT 字幕文件
            // 给 AI 提供 SRT 翻译前的语种 翻译后的语种 简述一下讲了什么内容
            ThrowUtils.throwIf(StrUtil.isBlank(optimizeSrt), ErrorCode.SYSTEM_ERROR, "AI 翻译内容为空");

            srtTranslate = callChatClientManager.callChatClient(optimizeSrt, briefExplanation,
                    textToSpeechLanguageCode, translationToLangCode);
        }

        // 删除本机临时文件
        DeleteFileUtil.deleteFileIfExists(outputVideoFilePath);
        DeleteFileUtil.deleteFileIfExists(outputAudioFilePath);

        // 返回文件下载流
        ReturnFileDownloadUtil.downloadTextContent(srtTranslate,
                IdUtil.simpleUUID() + SubtitlesConstant.SUBTITLES_FILE_FORMAT, response);
    }
}
