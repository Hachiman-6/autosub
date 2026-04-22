package com.djx.autosub.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SrtOptimizer {

    // 语言 -> 句子结束标点映射（正则字符集）
    private static final Map<String, String> LANGUAGE_END_PUNCTUATION = new HashMap<>();

    static {
        // 中文：中文标点 + 英文标点 + 省略号
        LANGUAGE_END_PUNCTUATION.put("zh-CN", "[.?!。？！！」」\"]+$");

        // 英文（美国、英国、印度）：标准英文标点
        for (String lang : Arrays.asList("en-US", "en-GB", "en-IN")) {
            LANGUAGE_END_PUNCTUATION.put(lang, "[.?!…\"]+$");
        }

        // 日语
        LANGUAGE_END_PUNCTUATION.put("ja-JP", "[.?!。？！…」］\"]+$");

        // 韩语
        LANGUAGE_END_PUNCTUATION.put("ko-KR", "[.?!。…\"]+$");

        // 德语、西班牙语（西班牙/墨西哥）、法语、意大利语、葡萄牙语（巴西）
        for (String lang : Arrays.asList("de-DE", "es-ES", "es-MX", "fr-FR", "it-IT", "pt-BR")) {
            LANGUAGE_END_PUNCTUATION.put(lang, "[.?!…\"]+$");
        }

        // 印地语：使用 । 作为句号
        LANGUAGE_END_PUNCTUATION.put("hi-IN", "[.?!।…\"]+$");
    }

    // 正则匹配时间轴行
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}:\\d{2}:\\d{2},\\d{3}) --> (\\d{2}:\\d{2}:\\d{2},\\d{3})");

    /**
     * 优化 SRT 字幕文本，并保存到本地文件（支持按语种判断句子结束）（短资源）
     *
     * @param srtContent 输入的 SRT 文本内容（String）
     * @param langCode   语种代码，如 "zh-CN", "en-US" 等
     * @return 优化后的 SRT 字幕文本
     */
    public static String optimizeSrt(String srtContent, String langCode) {

        // 1. 获取语义单元列表
        List<SubtitleItem> subtitleItemList = getSubtitleItems(srtContent, langCode);

        // 2. 构建输出文本 StringBuilder
        StringBuilder sb = getStringBuilder(subtitleItemList);

        // 3. 返回优化后的 SRT 字幕文本（String）
        return sb.toString();
    }

    /**
     * 优化 SRT 字幕文本，并保存到本地文件（支持按语种判断句子结束）（长资源）
     *
     * @param srtContentList     输入的 SRT 文本内容列表
     * @param langCode           语种代码，如 "zh-CN", "en-US" 等
     * @param timeBreakpointList 时间分割点列表
     * @param isTranslate    是否需要翻译
     * @return 优化后的 SRT 字幕文本列表
     */
    public static List<String> optimizeSrt(List<String> srtContentList, String langCode, List<Integer> timeBreakpointList, boolean isTranslate) {

        // 计算防止调用阿里云 AI 超时卡住的分割份数
        double copies;
        int srtContentBytesLength = 0;

        // 获取语义单元列表
        List<SubtitleItem> subtitleItemList = new ArrayList<>();
        for (int i = 0; i < srtContentList.size(); i++) {
            List<SubtitleItem> subtitleItemPartList;
            if (i == 0) {
                subtitleItemPartList = getSubtitleItems(srtContentList.get(i), langCode);
            } else {
                subtitleItemPartList = getSubtitleItems(srtContentList.get(i), langCode, timeBreakpointList.get(i - 1));
            }
            CollUtil.addAll(subtitleItemList, subtitleItemPartList);
            srtContentBytesLength += StrUtil.bytes(srtContentList.get(i), "UTF-8").length;
        }

        StringBuilder sb;
        // 如果不需要翻译，则把优化后的 SRT 字幕文本返回或写入本地文件
        if (!isTranslate) {
            sb = getStringBuilder(subtitleItemList);
            return CollUtil.newArrayList(sb.toString());
        }

        // 分割，防止调用阿里云 AI 超时卡住
        copies = Math.ceil(srtContentBytesLength / 6000.0);
        if (copies != 1.0) {
            // 如果份数不等于 1，才进行分割
            int partSize = (int) Math.ceil((double) subtitleItemList.size() / copies); // 每份最多 ceil(size/3) 个
            // 使用 Hutool 按每份最大数量分割
            List<List<SubtitleItem>> splitSubtitleItemListList = CollUtil.split(subtitleItemList, partSize);

            // 构建输出 SRT 文本列表并返回
            return getStringListForListList(splitSubtitleItemListList);
        } else {
            // 如果份数等于 1，不分割，返回单个完整的 SRT 文本元素的 List
            sb = getStringBuilder(subtitleItemList);
            return CollUtil.newArrayList(sb.toString());
        }
    }

    /**
     * 获取语义单元列表
     *
     * @param srtContent SRT文本内容
     * @param langCode   语种代码
     * @return 语义单元列表
     */
    private static List<SubtitleItem> getSubtitleItems(String srtContent, String langCode) {
        return getSubtitleItems(srtContent, langCode, null);
    }

    /**
     * 获取语义单元列表
     *
     * @param srtContent   SRT文本内容
     * @param langCode     语种代码
     * @param secondsToAdd 添加的秒数（保证分割后的 SRT 加上分割点的起始时间，可为空）
     * @return 语义单元列表
     */
    private static List<SubtitleItem> getSubtitleItems(String srtContent, String langCode, Integer secondsToAdd) {
        if (StrUtil.isBlank(srtContent)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "SRT 字幕文本为空");
        }

        // 获取对应语言的句子结束正则，未匹配则默认使用英文
        String punctPattern = LANGUAGE_END_PUNCTUATION.getOrDefault(langCode, "[.?!…\"]+$");
        Pattern sentenceEndPattern = Pattern.compile(punctPattern);

        // 1. 按行分割
        List<String> lines = StrUtil.split(srtContent, '\n');
        if (lines == null) lines = new ArrayList<>();

        // 2. 解析原始字幕项
        List<SubtitleItem> items = parseSrt(lines);

        // 3. 合并句子（根据语种标点）
        return mergeSentences(items, sentenceEndPattern, secondsToAdd);
    }

    /**
     * 构建输出文本 StringBuilder
     *
     * @param subtitleItemList 语义单元列表
     * @return 输出文本 StringBuilder
     */
    private static StringBuilder getStringBuilder(List<SubtitleItem> subtitleItemList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subtitleItemList.size(); i++) {
            SubtitleItem item = subtitleItemList.get(i);
            sb.append(i + 1).append("\n")
                    .append(item.startTime).append(" --> ").append(item.endTime).append("\n")
                    .append(StrUtil.trim(item.text)).append("\n\n");
        }

        // 移除末尾多余空行
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb;
    }

    /**
     * 构建输出文本 String 对于语义单元部分列表列表
     *
     * @param subtitleItemListList 语义单元部分列表列表
     * @return 输出文本 String 列表
     */
    private static List<String> getStringListForListList(List<List<SubtitleItem>> subtitleItemListList) {

        List<String> textList = new ArrayList<>();

        int i = 0;
        for (List<SubtitleItem> subtitleItemList : subtitleItemListList) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < subtitleItemList.size(); i++, j++) {
                SubtitleItem item = subtitleItemList.get(j);
                sb.append(i + 1).append("\n")
                        .append(item.startTime).append(" --> ").append(item.endTime).append("\n")
                        .append(StrUtil.trim(item.text)).append("\n\n");
            }
            // 移除末尾多余空行
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            textList.add(sb.toString());
        }
        return textList;
    }

    /**
     * 解析 SRT 行为 SubtitleItem 列表
     */
    private static List<SubtitleItem> parseSrt(List<String> lines) {
        List<SubtitleItem> items = new ArrayList<>();
        SubtitleItem currentItem = null;

        for (String line : lines) {
            line = StrUtil.trim(line);

            if (StrUtil.isEmpty(line)) {
                if (currentItem != null) {
                    items.add(currentItem);
                    currentItem = null;
                }
            } else if (ReUtil.isMatch("^\\d+$", line)) {
                continue; // 忽略序号
            } else if (ReUtil.isMatch(TIME_PATTERN, line)) {
                Matcher matcher = TIME_PATTERN.matcher(line);
                if (matcher.find()) {
                    currentItem = new SubtitleItem();
                    currentItem.startTime = matcher.group(1);
                    currentItem.endTime = matcher.group(2);
                }
            } else if (currentItem != null) {
                if (StrUtil.isEmpty(currentItem.text)) {
                    currentItem.text = line;
                } else {
                    currentItem.text += line;
                }
            }
        }

        if (currentItem != null) {
            items.add(currentItem);
        }

        return items;
    }

    /**
     * 合并断句（使用传入的 sentenceEndPattern 判断是否为完整句）
     *
     * @param items              语义单元列表
     * @param sentenceEndPattern 句子结束正则
     * @param secondsToAdd       添加的秒数（保证分割后的 SRT 加上分割点的起始时间，可为空）
     * @return 合并后的语义单元列表
     */
    private static List<SubtitleItem> mergeSentences(List<SubtitleItem> items, Pattern sentenceEndPattern, Integer secondsToAdd) {

        List<SubtitleItem> merged = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        String startTime = null;
        String endTime = null;

        for (SubtitleItem item : items) {
            if (startTime == null) {
                if (secondsToAdd == null) {
                    startTime = item.startTime;
                } else {
                    startTime = addSecondsToSrtTime(item.startTime, secondsToAdd);
                }
            }
            if (secondsToAdd == null) {
                endTime = item.endTime;
            } else {
                endTime = addSecondsToSrtTime(item.endTime, secondsToAdd);
            }

            currentText.append(item.text.trim());
            String text = currentText.toString().trim();

            // 使用语言特定的正则判断是否为完整句子
            if (sentenceEndPattern.matcher(text).find()) {
                SubtitleItem mergedItem = new SubtitleItem();
                mergedItem.startTime = startTime;
                mergedItem.endTime = endTime;
                mergedItem.text = text;
                merged.add(mergedItem);

                // 重置
                currentText.setLength(0);
                startTime = null;
                endTime = null;
            }
        }

        // 处理最后一句（未以标点结尾）
        if (!currentText.isEmpty()) {
            SubtitleItem lastItem = new SubtitleItem();
            lastItem.startTime = startTime != null ? startTime : "00:00:00,000";
            lastItem.endTime = endTime != null ? endTime : lastItem.startTime;
            lastItem.text = currentText.toString().trim();
            merged.add(lastItem);
        }

        return merged;
    }

    /**
     * 将 SRT 时间字符串加上指定的秒数，返回新的 SRT 时间字符串
     *
     * @param srtTime      SRT 时间字符串，格式如 "00:00:58,270"
     * @param secondsToAdd 要增加的秒数
     * @return 新的 SRT 时间字符串
     */
    private static String addSecondsToSrtTime(String srtTime, int secondsToAdd) {

        ThrowUtils.throwIf(secondsToAdd <= 0, ErrorCode.SYSTEM_ERROR, "分段 SRT 添加起始时间小于等于 0");

        // 解析原始时间字符串
        String[] timeParts = srtTime.split("[:,]");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        int seconds = Integer.parseInt(timeParts[2]);
        int milliseconds = Integer.parseInt(timeParts[3]);

        // 转换为总毫秒数
        long totalMillis = (hours * 3600L + minutes * 60L + seconds) * 1000 + milliseconds;

        // 加上要增加的秒数（转换为毫秒）
        totalMillis += secondsToAdd * 1000L;

        // 计算新的时间分量
        long totalSeconds = totalMillis / 1000;
        int newHours = (int) (totalSeconds / 3600);
        int newMinutes = (int) ((totalSeconds % 3600) / 60);
        int newSeconds = (int) (totalSeconds % 60);
        int newMilliseconds = (int) (totalMillis % 1000);

        // 格式化为 SRT 时间格式
        return String.format("%02d:%02d:%02d,%03d", newHours, newMinutes, newSeconds, newMilliseconds);
    }

    // 字幕项内部类
    private static class SubtitleItem {
        String startTime = "";
        String endTime = "";
        String text = "";
    }
}