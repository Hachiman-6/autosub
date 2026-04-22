package com.djx.autosub.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;

@Data
public class GetAudioFromVideoRequest implements Serializable {

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

    @Serial
    private static final long serialVersionUID = 1L;
}
