package com.djx.autosub.service;

import com.djx.autosub.model.dto.GetAiSrtFromVideoRequest;
import com.djx.autosub.model.dto.GetAudioFromVideoRequest;
import com.djx.autosub.model.dto.GetSrtFromVideoRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;

public interface FromVideoService {

    /**
     * 视频转音频并给客户端返回音频下载流
     *
     * @param getAudioFromVideoRequest 请求参数
     * @param response                 响应
     */
    void getAudioFromVideo(GetAudioFromVideoRequest getAudioFromVideoRequest, HttpServletResponse response);

    /**
     * 解析视频无需翻译返回 SRT 文件下载流
     *
     * @param getSrtFromVideoRequest 请求参数
     * @param response               响应
     */
    void getSrtFromVideo(GetSrtFromVideoRequest getSrtFromVideoRequest, HttpServletResponse response);

    /**
     * 解析视频并获得翻译后的 SRT
     *
     * @param getAiSrtFromVideoRequest 请求参数
     * @param response                 响应
     */
    void getTranslateSrtFromVideo(@RequestBody GetAiSrtFromVideoRequest getAiSrtFromVideoRequest,
                                  HttpServletResponse response);
}
