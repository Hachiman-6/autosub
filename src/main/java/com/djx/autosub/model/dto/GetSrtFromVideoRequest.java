package com.djx.autosub.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class GetSrtFromVideoRequest implements Serializable {

    /**
     * B 站 url
     */
    private String biliUrl;

    /**
     * youtube url
     */
    private String youtubeUrl;

    /**
     * 纯视频 url
     */
    private String videoUrl;

    /**
     * 用户上传的视频
     */
    private MultipartFile videoFile;

    /**
     * 调用网址 API 语种代码
     */
    private String textToSpeechLanguageCode;

    /**
     * 视频资源过长时，需截断的时间断点列表
     */
    private List<Integer> timeBreakpointList;

    @Serial
    private static final long serialVersionUID = 1L;
}