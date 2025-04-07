package nnu.mnr.satellite.utils.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/3/26 13:28
 * @Description:
 */
public class ConcurrentUtil {

    /**
     * 并发处理列表数据
     * @param items 输入数据列表
     * @param mapper 自定义的计算逻辑 (Tile -> JSONObject)
     * @param threadCount 线程数，默认使用 CPU 核心数
     * @param batchSize 每批处理的数据量
     * @param <T> 输入数据类型
     * @param <R> 输出数据类型
     * @return 处理结果列表
     * @throws IOException 如果计算过程中抛出异常
     */
    public static <T, R> List<R> processConcurrently(List<T> items, Function<T, R> mapper, int threadCount, int batchSize) throws IOException {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<List<R>>> futures = new ArrayList<>();

        try {
            // 分批提交任务
            for (int i = 0; i < items.size(); i += batchSize) {
                int end = Math.min(i + batchSize, items.size());
                List<T> batch = items.subList(i, end);

                Callable<List<R>> task = () -> batch.stream()
                        .map(item -> {
                            try {
                                return mapper.apply(item);
                            } catch (Exception e) {
                                throw new RuntimeException("Error processing item", e);
                            }
                        })
                        .collect(Collectors.toList());

                futures.add(executor.submit(task));
            }

            // 收集结果
            List<R> results = new ArrayList<>();
            for (Future<List<R>> future : futures) {
                try {
                    List<R> batchResult = future.get();
                    results.addAll(batchResult);
                } catch (InterruptedException | ExecutionException e) {
                    throw new IOException("Failed to process items concurrently", e);
                }
            }
            return results;
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // 重载方法，使用默认线程数和批次大小
    public static <T, R> List<R> processConcurrently(List<T> items,
                                                     Function<T, R> mapper) throws IOException {
        int defaultThreadCount = Runtime.getRuntime().availableProcessors();
        int defaultBatchSize = 1000;
        return processConcurrently(items, mapper, defaultThreadCount, defaultBatchSize);
    }

}
