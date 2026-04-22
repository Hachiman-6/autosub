package com.djx.autosub.util;

import com.djx.autosub.config.ThreadPoolConfig;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class AsyncNetworkCallCompletableFutureTest {

    @Test
    void createAsync() {

        AsyncNetworkCallCompletableFuture<String, Integer> future = new AsyncNetworkCallCompletableFuture<>() {
            @Override
            public AsyncNetworkCallCompletableFuture<String, Integer>.CallResult asyncTask(String callPara) {
                Integer s;
                try {
                    s = test1(callPara);
                } catch (Exception e) {
                    return new CallResult(null, e);
                }
                return new CallResult(s, null);
            }
        };
        List<String> stringList = new ArrayList<>();
        stringList.add("hello");
        stringList.add("world");
        stringList.add("addd");
        stringList.add("springboot");
        ThreadPoolConfig config = new ThreadPoolConfig();
        ExecutorService executorService = config.networkRequestThreadPool();
        List<Integer> async = future.createAsync(stringList, executorService);
        System.out.println(async);
    }

    private Integer test1(String str) throws InterruptedException {
        // 模拟不同耗时
        // Thread.sleep((long)(Math.random() * 1000));
        Thread.sleep(10000L);
        // return str.toUpperCase();
        if (str.length() == 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return str.length();
    }
}