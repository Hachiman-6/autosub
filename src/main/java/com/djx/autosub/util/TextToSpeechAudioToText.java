package com.djx.autosub.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class TextToSpeechAudioToText {

    /**
     * 利用 www.text-to-speech.cn/ 的产品获取初步按停顿分割的 SRT 字幕文件
     *
     * @param audio        音频文件
     * @param languageCode 音频语种
     * @param token        token
     * @return SRT 字幕文本
     */
    public static String audioToText(File audio, String languageCode, String token) {

        // 判断音频大小是否小于 200 MB（API 规定必须小于 200 MB）
        ThrowUtils.throwIf(FileUtil.size(audio) >= 200 * 1024 * 1024, ErrorCode.PARAMS_ERROR,
                "每段音频需小于 200MB，请调整文件大小");

        // 添加表单参数
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("video", audio);
        paramMap.put("language", languageCode);
        paramMap.put("token", token);

        // 获取 SRT 访问地址
        String body;
        try {
            body = HttpUtil.createPost("https://www.text-to-speech.cn/getSrt.php")
                    .timeout(300000)
                    .form(paramMap)
                    .execute()
                    .body();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "超时请重试");
        }

        JSONObject jsonObject = JSONUtil.parseObj(body);
        String download = jsonObject.getStr("download");
        String jsonUrl = jsonObject.getStr("url");
        if (StrUtil.isBlank(download) || StrUtil.isBlank(jsonUrl)) {
            // TODO 如果返回是请重试，前端需要重试
            log.info("调用生成字幕网站错误返回：" + body);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用生成字幕网站错误");
        }
        String finalUrl = jsonUrl.substring(0, jsonUrl.length() - 1) + download;

        // 访问 SRT 地址，返回 SRT 字幕文本
        return HttpUtil.createGet(finalUrl)
                .execute()
                .body();
    }

    /**
     * 获取 token
     *
     * @return token
     */
    public static String getToken() {
        // 获取 token
        // 获取 html（含 token）
        String html = HttpUtil.createGet("https://www.text-to-speech.cn/srt.html")
                .execute()
                .body();
        // 解析 html 获取 token
        return extractToken(html);
    }

    /**
     * 从 HTML 字符串中提取第 19 行的 token 值
     *
     * @param html HTML 字符串
     * @return token 值，未找到返回 null
     */
    private static String extractToken(String html) {
        if (StrUtil.isBlank(html)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "html 为空");
        }

        // 将字符串按行分割为 List
        List<String> lines = StrUtil.splitTrim(html, '\n');

        // 检查是否有至少 19 行
        if (lines.size() < 19) {
            log.error("HTML 行数不足 19 行，当前行数：" + lines.size());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析html错误");
        }

        // 获取第 19 行，有 token 的那一行（索引为 18）
        String tokenLine = lines.get(18);

        // 使用 Hutool 的正则提取工具提取单引号内的内容
        return ReUtil.getGroup1("const\\s+token\\s*=\\s*'([^']+)'", tokenLine);
    }
}
