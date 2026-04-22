package com.djx.autosub.springaialibaba.manager;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.model.enums.TextToSpeechWebLanguageEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CallChatClientManager {

    @Resource
    private ChatClient chatClientForTranslation;

    /**
     * 使用 ChatClient 在本地写入 AI 加工后 SRT 文件
     *
     * @param originalSrt      SRT 原始内容
     * @param briefExplanation 翻译的简要说明
     * @param fromLanguageCode 源语种代码
     * @param toLanguageCode   目标语种代码
     * @return 翻译后的 SRT 文本内容
     */
    public String callChatClient(String originalSrt, String briefExplanation, String fromLanguageCode, String toLanguageCode) {

        // 转化语言代码为文本描述
        String fromLanguageText;
        String toLanguageText;
        TextToSpeechWebLanguageEnum fromLanguageEnum = TextToSpeechWebLanguageEnum.getEnumByValue(fromLanguageCode);
        if (fromLanguageEnum != null) {
            fromLanguageText = fromLanguageEnum.getText();
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "SRT源语种不支持");
        }

        TextToSpeechWebLanguageEnum toLanguageEnum = TextToSpeechWebLanguageEnum.getEnumByValue(toLanguageCode);
        if (toLanguageEnum != null) {
            toLanguageText = toLanguageEnum.getText();
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "SRT目标语种不支持");
        }

        String userPrompt = getTranslationRequest(fromLanguageText, toLanguageText, briefExplanation, originalSrt);

        return chatClientForTranslation.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * 生成指定格式的翻译请求文本（使用 Hutool 优化）
     *
     * @param sourceLang  源语言
     * @param targetLang  目标语言
     * @param contextInfo 辅助翻译的上下文信息，可为 null
     * @param srtContent  SRT 内容
     * @return 格式化后的字符串（格式 A 或格式 B）
     */
    private String getTranslationRequest(String sourceLang, String targetLang,
                                         String contextInfo, String srtContent) {
        StrBuilder sb = new StrBuilder();

        // 添加语种信息
        sb.append("语种：由").append(sourceLang).append("翻译为").append(targetLang).append("\n");

        // 如果 contextInfo 非空，则使用格式 A
        if (StrUtil.isNotBlank(contextInfo)) {
            sb.append("简要说明：").append(contextInfo).append("\n");
        }

        // 添加 SRT 文本
        sb.append("SRT文本如下：\n").append(srtContent);

        return sb.toString();
    }
}