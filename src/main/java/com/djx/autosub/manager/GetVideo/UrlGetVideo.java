package com.djx.autosub.manager.GetVideo;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.manager.GetVideo.GetVideoInterface;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class UrlGetVideo implements GetVideoInterface {

    /**
     * 获取视频下载链接（例如：阿里云 OSS）
     *
     * @param videoResource 视频 URL
     * @return 视频下载输入流
     */
    @Override
    public InputStream getVideo(Object videoResource) {
        if (!(videoResource instanceof String)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        String videoUrl = (String) videoResource;
        this.checkVideoUrl(videoUrl);
        return HttpUtil.createGet(videoUrl).execute().bodyStream();
    }

    private void checkVideoUrl(String videoUrl) {
        // 1. 先处理URL，去除查询参数后再检查扩展名
        String cleanUrl = removeQueryParams(videoUrl).toLowerCase();

        // 常见视频文件扩展名
        String[] videoExtensions = {".mp4", ".avi", ".mov", ".flv", ".wmv", ".mkv", ".webm"};

        // 检查处理后的URL是否以视频扩展名结尾
        for (String ext : videoExtensions) {
            if (cleanUrl.endsWith(ext)) {
                return;
            }
        }

        // 2. 如果扩展名不确定，使用Hutool发送HEAD请求检查Content-Type
        try {
            HttpResponse response = HttpRequest.head(videoUrl)
                    .timeout(5000) // 设置超时时间5秒
                    .execute();

            if (response.isOk()) {
                String contentType = response.header("Content-Type");
                boolean headResult = contentType != null && contentType.startsWith("video/");
                if (headResult) {
                    return;
                }
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网址错误");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网址错误");
        }
    }

    private static String removeQueryParams(String url) {
        int queryIndex = url.indexOf('?');
        return queryIndex > 0 ? url.substring(0, queryIndex) : url;
    }
}
