package com.djx.autosub;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

//@SpringBootTest
@Slf4j
class AutosubApplicationTests {

    @Test
    void contextLoads() {
        List<String> split = StrUtil.split("https://www.bilibili.com/video/BV1Ln3SzGELz/?spm_id_from=333.1007.tianma.2-1-4.click&vd_source=6e00c4a49f70af02a78d1a68112900ce", '/');
        String s = split.get(4);
        int i = s.indexOf('?');
        if(i != -1){
            s = s.substring(0, i);
        }
        System.out.println(s);
    }

}
