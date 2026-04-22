package com.djx.autosub.service;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.StringJoiner;

//@SpringBootTest
class CommonTest {

    @Test
    void test(){

        String html = "";

        if (StrUtil.isBlank(html)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "html 为空");
        }

        // 将字符串按行分割为 List
        List<String> lines = StrUtil.splitTrim(html, '\n');

        // 检查是否有至少 19 行
        if (lines.size() < 19) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析html错误");
        }

        // 获取第 19 行，有 token 的那一行（索引为 18）
        String tokenLine = lines.get(18);

        // 使用 Hutool 的正则提取工具提取单引号内的内容
        String result = ReUtil.getGroup1("const\\s+token\\s*=\\s*'([^']+)'", tokenLine);

        System.out.println(result);
    }

    @Test
    void test1(){
        StringJoiner sj = new StringJoiner(",");
        String string = sj.toString();
        System.out.println(string);
        System.out.println(string == null);
        System.out.println(StrUtil.isBlank(string));
    }
}