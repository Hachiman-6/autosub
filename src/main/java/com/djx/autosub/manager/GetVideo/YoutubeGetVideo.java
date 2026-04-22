package com.djx.autosub.manager.GetVideo;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@Slf4j
public class YoutubeGetVideo implements GetVideoInterface {

    /**
     * 获取 Youtube 视频下载输入流
     *
     * @param videoResource YouTube 视频 URL
     * @return 视频下载输入流
     */
    @Override
    public InputStream getVideo(Object videoResource) {
        if (!(videoResource instanceof String)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        String youtubeUrl = (String) videoResource;
        // 校验是否为 ytb 站的 url 并获取视频下载网址
        String ytbVideoUrl = getVideoDownloadUrl(youtubeUrl);
        // 获取音频下载链接 String ytbAudioUrl = formats.getJSONObject(8).getStr("url");
        // 返回视频流
        HttpResponse response = HttpUtil.createGet(ytbVideoUrl)
                .setFollowRedirects(true)
                .execute();
        if (!response.isOk()) { // 状态码不是 200-299
            log.error("Youtube视频访问失败，状态码Status：" + response.getStatus());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频访问失败");
        }
        return response.bodyStream();
    }

    /**
     * 校验是否为 ytb 站的 url 并获取视频下载网址
     *
     * @param youtubeUrl youtubeUrl
     * @return 视频下载网址
     */
    public String getVideoDownloadUrl(String youtubeUrl) {
        // 校验是否是 Youtube 的视频网址
        // https://www.youtube.com/watch?v=jNQXAC9IVRw
        this.checkBiliUrl(youtubeUrl);
        String body;
        try {
            body = HttpUtil.createGet("https://youtubetomp4.pro/api/get-formats")
                    .form("url", youtubeUrl)
                    .execute()
                    .body();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "网络连接中断");
        }
        JSONObject jsonObj = JSONUtil.parseObj(body);
        JSONArray formats = jsonObj.getJSONArray("formats");
        ThrowUtils.throwIf(formats == null, ErrorCode.PARAMS_ERROR, "youTube网址错误");
        // 获取视频下载链接
        return formats.getJSONObject(0).getStr("url");
    }

    /**
     * 校验是否是 YouTube 的视频链接
     *
     * @param youtubeUrl YouTube 视频 URL
     */
    private void checkBiliUrl(String youtubeUrl) {
        // https://www.youtube.com/watch?v=jNQXAC9IVRw
        // https://www.youtube.com/watch?v=XRvUWP6VaJY
        if (!youtubeUrl.startsWith("https://www.youtube.com/watch?v=")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网址错误");
        }
    }
}
