package com.djx.autosub.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum ResourceLanguageEnum {

    CHINESE("汉语", "chinese"),
    ENGLISH("英语", "english");

    private final String text;

    private final String value;

    ResourceLanguageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static ResourceLanguageEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ResourceLanguageEnum anEnum : ResourceLanguageEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
