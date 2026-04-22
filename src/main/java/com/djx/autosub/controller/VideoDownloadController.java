package com.djx.autosub.controller;

import com.djx.autosub.common.BaseResponse;
import com.djx.autosub.common.ResultUtils;
import com.djx.autosub.manager.GetVideo.BiliGetVideo;
import com.djx.autosub.manager.GetVideo.YoutubeGetVideo;
import com.djx.autosub.model.dto.GetBiliDownloadUrlRequest;
import com.djx.autosub.model.dto.GetYtbDownloadUrlRequest;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/videoload")
public class VideoDownloadController {

    @Resource
    private BiliGetVideo biliGetVideo;

    @Resource
    private YoutubeGetVideo youtubeGetVideo;

    /**
     * 获取视频 B 站下载网址
     */
    @PostMapping("/getbiliurl")
    public BaseResponse<String> getBiliDownloadUrl(@RequestBody GetBiliDownloadUrlRequest getBiliDownloadUrlRequest) {
        String videoDownloadUrl = biliGetVideo.getVideoDownloadUrl(getBiliDownloadUrlRequest.getBiliUrl());
        return ResultUtils.success(videoDownloadUrl);
    }

    /**
     * 获取视频 youtube 下载网址
     */
    @PostMapping("/getyoutubeurl")
    public BaseResponse<String> getYoutubeDownloadUrl(@RequestBody GetYtbDownloadUrlRequest getYtbDownloadUrlRequest) {
        String videoDownloadUrl = youtubeGetVideo.getVideoDownloadUrl(getYtbDownloadUrlRequest.getYoutubeUrl());
        return ResultUtils.success(videoDownloadUrl);
    }
}
