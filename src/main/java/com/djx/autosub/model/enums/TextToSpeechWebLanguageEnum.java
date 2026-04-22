package com.djx.autosub.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * TextToSpeech 网站语言枚举类
 */
@Getter
public enum TextToSpeechWebLanguageEnum {

    ZH_CN("中文（简体）", "zh-CN"),
    EN_US("英语（美国）", "en-US"),
    JA_JP("日语（日本）", "ja-JP"),
    KO_KR("韩语（韩国）", "ko-KR"),
    DE_DE("德语（德国）", "de-DE"),
    EN_GB("英语（英国）", "en-GB"),
    EN_IN("英语（印度）", "en-IN"),
    ES_ES("西班牙语（西班牙）", "es-ES"),
    ES_MX("西班牙语（墨西哥）", "es-MX"),
    FR_FR("法语（法国）", "fr-FR"),
    HI_IN("印地语（印度）", "hi-IN"),
    IT_IT("意大利语（意大利）", "it-IT"),
    PT_BR("葡萄牙语（巴西）", "pt-BR");

    /**
     * 显示名称，例如：中文（简体）
     */
    private final String text;

    /**
     * 语言代码，例如：zh-CN
     */
    private final String value;


    TextToSpeechWebLanguageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static TextToSpeechWebLanguageEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (TextToSpeechWebLanguageEnum anEnum : TextToSpeechWebLanguageEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 检查指定的语言代码是否存在
     * @param value 语言代码
     * @return 如果存在返回 true，否则返回 false
     */
    public static boolean containsCode(String value) {
        return getEnumByValue(value) != null;
    }
}
