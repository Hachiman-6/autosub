package com.djx.autosub.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class GetBiliDownloadUrlRequest implements Serializable {

    /**
     * B 站的视频网址
     */
    private String biliUrl;

    @Serial
    private static final long serialVersionUID = 1L;
}
