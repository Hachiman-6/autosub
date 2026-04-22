package com.djx.autosub.manager.GetVideo;

import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.manager.GetVideo.GetVideoInterface;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Component
public class MultipartFileGetVideo implements GetVideoInterface {
    @Override
    public InputStream getVideo(Object videoResource) {
        if (!(videoResource instanceof MultipartFile)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        MultipartFile videoFile = (MultipartFile) videoResource;
        this.checkIsVideo(videoFile);
        InputStream inputStream;
        try {
            inputStream = videoFile.getInputStream();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件错误" + e);
        }
        return inputStream;
    }

    private void checkIsVideo(MultipartFile videoFile) {
        if (videoFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件错误");
        }
        // 1. 先检查Content-Type
        String contentType = videoFile.getContentType();
        if (contentType.startsWith("video/")) {
            return;
        }
        // 2. 如果Content-Type不确定，再检查扩展名
        String fileName = videoFile.getOriginalFilename().toLowerCase();
        String[] videoExtensions = {".mp4", ".avi", ".mov", ".flv", ".wmv", ".mkv", ".webm"};

        for (String ext : videoExtensions) {
            if (fileName.endsWith(ext)) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件错误");
    }
}
