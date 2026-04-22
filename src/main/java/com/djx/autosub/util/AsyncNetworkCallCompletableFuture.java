package com.djx.autosub.util;

import cn.hutool.core.util.StrUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * 异步多线程调用类
 *
 * @param <T> 调用参数
 * @param <U> 返回结果
 */
@Slf4j
public abstract class AsyncNetworkCallCompletableFuture<T, U> {

    /**
     * CallResult 返回结果内部类
     */
    @Getter
    public class CallResult {
        private final U result;
        private final Exception exception;

        public CallResult(U result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        public boolean isSuccess() {
            return exception == null;
        }

        @Override
        public String toString() {
            if (isSuccess()) {
                return String.format("Result: '%s'", result);
            } else {
                return String.format("FAILED: %s", exception.getMessage());
            }
        }
    }

    /**
     * 可执行的异步任务（抽象方法，需重写）
     *
     * @param callPara 调用参数
     * @return 返回结果内部类 CallResult
     */
    public abstract CallResult asyncTask(T callPara);

    /**
     * 创建异步任务（多线程）
     *
     * @param callList 调用参数列表
     * @return 返回结果列表
     */
    public List<U> createAsync(List<T> callList, ExecutorService networkRequestThreadPool) {

        log.info("开始并行处理 {} 个资源...", callList.size());

        List<CallResult> results;
        try {
            // 2. 为每个文本创建一个 CompletableFuture
            //    使用 supplyAsync 并指定 executor
            List<CompletableFuture<CallResult>> futures = callList.stream()
                    .map(resource -> CompletableFuture.supplyAsync(() -> asyncTask(resource), networkRequestThreadPool))
                    .toList();

            // 3. 创建一个 CompletableFuture，当所有任务都完成后才完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // 4. 主线程阻塞等待所有任务完成
            //    get() 会抛出异常，需要处理
            allOf.get(); // 这里会等待所有任务结束

            // 所有任务已完成，主线程继续
            log.info("所有异步调用已完成！");

            // 5. 获取每个 future 的结果 (此时不会阻塞)
            results = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(); // 此处 get() 不会阻塞
                        } catch (InterruptedException | ExecutionException e) {
                            // 理论上不会到这里，因为我们的 Callable 已经捕获了异常
                            Thread.currentThread().interrupt();
                            return new CallResult(null, e);
                        }
                    })
                    .toList();

            // 6. 处理结果，抛出异常
            StringJoiner sj = new StringJoiner(",");
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i).isSuccess()) {
                    log.info("第 {} 段资源调用线程成功", i + 1);
                } else {
                    // String.format("FAILED: %s", exception.getMessage());
                    String logStr = String.format("第 %s 段资源调用线程失败，失败信息：%s", i + 1, results.get(i).getException().getMessage());
                    log.error(logStr);
                    sj.add(logStr);
                }
            }
            if (!StrUtil.isBlank(sj.toString())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, sj.toString());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("主线程被中断: " + e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        } catch (ExecutionException e) {
            log.error("CompletableFuture.allOf 执行异常: " + e.getCause().getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }

        return results.stream()
                .map(CallResult::getResult)
                .toList();
    }
}