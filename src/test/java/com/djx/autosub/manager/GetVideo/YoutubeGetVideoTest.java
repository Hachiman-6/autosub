package com.djx.autosub.manager.GetVideo;

import cn.hutool.core.util.IdUtil;
import com.djx.autosub.constant.VideoConstant;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class YoutubeGetVideoTest {

    @Test
    void test() {
        YoutubeGetVideo youtubeGetVideo = new YoutubeGetVideo();
        InputStream inputStream = youtubeGetVideo.getVideo("https://www.youtube.com/watch?v=jNQXAC9IVRw");
        // 下载视频
        ThrowUtils.throwIf(inputStream == null, ErrorCode.SYSTEM_ERROR);
        String outputVideoFilePath = VideoConstant.VIDEO_STORAGE_FOLDER_ORIGINAL + IdUtil.simpleUUID() + VideoConstant.VIDEO_FILE_FORMAT;
        File outputVideoFile = new File(outputVideoFilePath);
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(outputVideoFile);
        } catch (FileNotFoundException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bufferedInputStream.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频写入硬盘失败" + e.getMessage());
        }
    }
}