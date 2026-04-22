package com.djx.autosub.controller;

import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import com.djx.autosub.model.dto.GetAiSrtFromVideoRequest;
import com.djx.autosub.model.dto.GetAudioFromVideoRequest;
import com.djx.autosub.model.dto.GetSrtFromVideoRequest;
import com.djx.autosub.service.FromVideoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fromvideo")
public class fromVideoController {

    @Resource
    private FromVideoService fromVideoService;

    /**
     * 将用户的视频转化为音频并返回音频下载流
     */
    @PostMapping("/audio")
    public void getAudioFromVideo(GetAudioFromVideoRequest getAudioFromVideoRequest, HttpServletResponse response) {
        ThrowUtils.throwIf(getAudioFromVideoRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        fromVideoService.getAudioFromVideo(getAudioFromVideoRequest, response);
    }

    /**
     * 根据用户的视频生成 SRT 文件并返回下载流
     */
    @PostMapping("/srt")
    public void getSrtFromVideo(GetSrtFromVideoRequest getSrtFromVideoRequest, HttpServletResponse response) {
        ThrowUtils.throwIf(getSrtFromVideoRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        fromVideoService.getSrtFromVideo(getSrtFromVideoRequest, response);
    }

    /**
     * 根据用户的视频生成翻译后的 SRT 文件并返回下载流
     */
    @PostMapping("/srt/translate")
    public void getSrtFromVideo(GetAiSrtFromVideoRequest getAiSrtFromVideoRequest, HttpServletResponse response) {
        ThrowUtils.throwIf(getAiSrtFromVideoRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        fromVideoService.getTranslateSrtFromVideo(getAiSrtFromVideoRequest, response);
    }
}
