package com.djx.autosub.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.djx.autosub.exception.BusinessException;
import com.djx.autosub.exception.ErrorCode;
import com.djx.autosub.exception.ThrowUtils;
import jakarta.servlet.ServletOutputStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件下载工具类，提供静态方法来处理服务器文件的下载请求。
 */
public class ReturnFileDownloadUtil {

    /**
     * 根据服务器上的文件路径，处理文件下载请求。
     * 此方法会直接向 HttpServletResponse 写入文件流。
     *
     * @param filePathStr 服务器上文件的路径
     * @param response    HttpServletResponse 对象，用于写出响应
     */
    public static void downloadFile(String filePathStr, HttpServletResponse response) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(filePathStr), ErrorCode.PARAMS_ERROR, "文件路径不能为空");

        // 转化为 Path 类型
        Path filePath = Paths.get(filePathStr);

        // 检查文件是否存在且是常规文件
        ThrowUtils.throwIf(!Files.exists(filePath) || !Files.isRegularFile(filePath),
                ErrorCode.NOT_FOUND_ERROR, "请求的文件不存在或不是有效文件: " + filePath);

        try {
            // 创建 Resource 对象
            Resource resource = new UrlResource(filePath.toUri());

            // 探测文件的 MIME 类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                // 如果无法探测，则根据常见音视频扩展名设置默认类型
                String fileName = filePath.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".aac")) {
                    contentType = "audio/mpeg";
                } else if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
                    contentType = "video/mp4"; // 简化处理
                } else {
                    contentType = "application/octet-stream";
                }
            }

            // 设置响应头
            response.setContentType(contentType);
            response.setContentLengthLong(Files.size(filePath));

            // Content-Disposition: attachment 表示作为附件下载
            String headerKey = "Content-Disposition";
            String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";
            response.setHeader(headerKey, headerValue);

            // 可选：添加缓存控制
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            // 优先使用 NIO 零拷贝（仅本地文件可用）
            try (// 1. 打开文件通道 (FileChannel)
                 FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);
                 // 2. 获取响应的输出流 (ServletOutputStream)
                 ServletOutputStream outputStream = response.getOutputStream();
                 // 3. 将输出流包装为通道 (WritableByteChannel)
                 WritableByteChannel outputChannel = Channels.newChannel(outputStream)) {

                fileChannel.transferTo(0, fileChannel.size(), outputChannel);

            } catch (IOException e) {
                // fallback 到普通流复制（如路径非本地文件）
                try (InputStream inputStream = resource.getInputStream()) {
                    IoUtil.copy(inputStream, response.getOutputStream(), 32 * 1024);
                }
            }

            // 成功时无需返回值，流已写出
        } catch (MalformedURLException e) {
            // UrlResource 构造异常，属于系统问题
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件资源URL构建失败");
        } catch (IOException e) {
            // 文件读取或网络IO异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件读取或传输过程中发生IO异常: " + e.getMessage());
        }
    }

    /**
     * 下载文本内容（支持 .srt 字幕文件）
     *
     * @param content  文本内容
     * @param filename 下载文件名
     * @param response HttpServletResponse 对象
     */
    public static void downloadTextContent(String content, String filename, HttpServletResponse response) {
        if (content == null) {
            content = "";
        }
        if (filename == null || filename.trim().isEmpty()) {
            filename = "download.srt";
        }
        filename = filename.trim();

        try {
            // 1. UTF-8 编码
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(bytes);

            // 2. 设置响应头
            // 对 .srt 使用 text/plain（兼容性最好）
            response.setContentType("text/plain; charset=UTF-8");
            response.setContentLength(bytes.length);

            // 确保文件名正确
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            // 3. 使用 Hutool 传输
            ServletOutputStream outputStream = response.getOutputStream();
            IoUtil.copy(inputStream, outputStream, 32 * 1024); // 32KB 缓冲区

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文本下载失败: " + e.getMessage());
        }
    }
}