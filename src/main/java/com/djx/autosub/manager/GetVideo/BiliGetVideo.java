package com.djx.autosub.manager.GetVideo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;

@Slf4j
@Component
public class BiliGetVideo implements GetVideoInterface {

    /**
     * 获取 B站视频下载输入流
     *
     * @param videoResource B站视频 URL
     * @return 视频下载输入流
     */
    @Override
    public InputStream getVideo(Object videoResource) {
        if (!(videoResource instanceof String)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        String biliUrl = (String) videoResource;
        // 校验是否为 B 站的 url 并获取视频下载网址
        String finalUrl = getVideoDownloadUrl(biliUrl);
        HttpResponse response = HttpUtil.createGet(finalUrl)
                .execute();
        if (!response.isOk()) { // 状态码不是 200-299
            log.error("B 站视频访问失败，状态码Status：" + response.getStatus());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "视频访问失败");
        }
        return response.bodyStream();
    }

    /**
     * 校验是否为 B 站的 url 并获取视频下载网址
     *
     * @param biliUrl B站视频 URL
     * @return 视频下载网址
     */
    public String getVideoDownloadUrl(String biliUrl) {
        // 校验是否是 B 站的视频网址
        this.checkBiliUrl(biliUrl);
        // https://www.bilibili.com/video/BV1Ln3SzGELz?t=5.7
        // https://www.bilibili.com/video/BV1Ln3SzGELz/?spm_id_from=333.1007.tianma.2-1-4.click&vd_source=6e00c4a49f70af02a78d1a68112900ce
        String bv = this.getBV(biliUrl);
        String body;
        try {
            body = HttpUtil.createGet("https://api.bilibili.com/x/web-interface/view?bvid=" + bv)
                    .execute()
                    .body();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "网络连接中断");
        }
        JSONObject jsonObj = JSONUtil.parseObj(body);
        JSONObject data = jsonObj.getJSONObject("data");
        ThrowUtils.throwIf(data == null, ErrorCode.NOT_FOUND_ERROR);
        String aid = data.getStr("aid");
        String cid = data.getStr("cid");
        String body2;
        try {
            body2 = HttpUtil.createGet("https://api.bilibili.com/x/player/playurl?avid=" + aid + "&cid=" + cid + "&qn=80&type=mp4&platform=html5&high_quality=1")
                    .execute()
                    .body();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "网络连接中断");
        }
        JSONObject jsonObj2 = JSONUtil.parseObj(body2);
        JSONObject data2 = jsonObj2.getJSONObject("data");
        String durl = data2.getStr("durl");
        String subDurl = durl.substring(1, durl.length() - 1);
        JSONObject durlObj = JSONUtil.parseObj(subDurl);
        return durlObj.getStr("url");
    }

    /**
     * 获取 BV 号
     *
     * @param biliUrl B站视频 URL
     * @return BV 号
     */
    private String getBV(String biliUrl) {
        List<String> urlSplit = StrUtil.split(biliUrl, '/');
        String bv = urlSplit.get(4);
        int index = bv.indexOf('?');
        if (index != -1) {
            bv = bv.substring(0, index);
        }
        ThrowUtils.throwIf(!bv.startsWith("BV"), ErrorCode.PARAMS_ERROR, "网址错误");
        return bv;
    }

    /**
     * 校验是否是 B 站的视频网址
     *
     * @param biliUrl B站视频 URL
     */
    private void checkBiliUrl(String biliUrl) {
        if (!biliUrl.startsWith("https://www.bilibili.com/video/")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网址错误");
        }
    }

    /**
     * 本地下载
     *
     * @param outputFilePath 输出文件路径
     * @param finalUrl       最终下载链接
     * @throws IOException 输入输出异常
     */
    @Deprecated
    private void localDownload(String outputFilePath, String finalUrl) throws IOException {
        // 服务器本地下载：使用 Hutool 的 HttpUtil 下载视频
        InputStream inputStream = HttpUtil.createGet(finalUrl).execute().bodyStream();
        // 将输入流写入到文件，使用BufferedInputStream和BufferedOutputStream提高速率
        File outputFile = new File(outputFilePath);
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile))) {

            byte[] buffer = new byte[8192]; // 可根据需要调整缓冲区大小
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
                bufferedOutputStream.write(buffer, 0, bytesRead);
            }
        }
        log.info("视频下载成功: " + outputFile.getAbsolutePath());
    }
}
