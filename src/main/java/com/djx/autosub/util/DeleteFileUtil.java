package com.djx.autosub.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class DeleteFileUtil {

    /**
     * 根据文件路径判断文件是否存在，如果存在则删除
     *
     * @param filePath 文件的路径
     */
    public static void deleteFileIfExists(String filePath) {
        // 检查传入路径是否为空
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("错误：文件路径不能为空！");
            return;
        }

        Path path = Paths.get(filePath);

        try {
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("文件删除成功：{}", filePath);
            } else {
                log.error("错误：文件不存在：{}", filePath);
            }
        } catch (Exception e) {
            log.error("错误：删除文件时发生异常：{}", e.getMessage());
        }
    }
}
