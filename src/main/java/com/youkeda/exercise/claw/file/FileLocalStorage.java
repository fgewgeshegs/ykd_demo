package com.youkeda.exercise.claw.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件存储实现
 *
 * <p>将文件保存到 {@code data/users/{userId}/files/original/} 目录下，
 * 以 {@code userId} 做目录级用户隔离。支持白名单校验和大小限制。
 *
 * <p>实现 {@link FileStorage} 接口，可替换为 OSS/S3 等远端存储。
 */
@Component
public class FileLocalStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(FileLocalStorage.class);

    /** 用户文件根目录 */
    static final String ORIGINAL_DIR = "files" + File.separator + "original";

    /** 用户文件根目录（可通过 file.storage-root 配置） */
    private String filesRoot;

    /** 允许的文件扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("md", "txt", "pdf", "docx");

    @Value("${file.max-size:20971520}")
    private long maxFileSize;

    public FileLocalStorage() {
        this.filesRoot = "data" + File.separator + "users";
    }

    /**
     * 设置用户文件根目录（由 Spring 从 file.storage-root 注入）
     */
    @Value("${file.storage-root:}")
    public void setFilesRoot(String root) {
        if (root != null && !root.isBlank()) {
            this.filesRoot = root;
        }
    }

    @Override
    public String save(String userId, byte[] content, String extension) {
        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: ." + extension);
        }
        if (!isWithinSizeLimit(content)) {
            throw new IllegalArgumentException("文件超过大小限制 (" + (maxFileSize / 1024 / 1024) + "MB)");
        }

        // 生成唯一存储名：UUID + 原始扩展名
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        Path targetPath = resolvePath(userId, storedName);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, content);
            log.info("文件已保存 | userId={} | storedName={} | size={}", userId, storedName, content.length);
            return storedName;
        } catch (IOException e) {
            log.error("文件保存失败 | userId={} | storedName={}", userId, storedName, e);
            throw new RuntimeException("文件保存失败", e);
        }
    }

    @Override
    public byte[] read(String userId, String storedName) {
        Path path = resolvePath(userId, storedName);
        if (!Files.exists(path)) {
            log.warn("文件不存在 | userId={} | storedName={}", userId, storedName);
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("文件读取失败 | userId={} | storedName={}", userId, storedName, e);
            return null;
        }
    }

    @Override
    public boolean delete(String userId, String storedName) {
        Path path = resolvePath(userId, storedName);
        try {
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("文件已删除 | userId={} | storedName={}", userId, storedName);
            }
            return deleted;
        } catch (IOException e) {
            log.error("文件删除失败 | userId={} | storedName={}", userId, storedName, e);
            return false;
        }
    }

    @Override
    public boolean isAllowedExtension(String extension) {
        return extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public boolean isWithinSizeLimit(byte[] content) {
        return content != null && content.length <= maxFileSize;
    }

    /**
     * 解析文件路径，并进行路径穿越防护
     *
     * <p>最终路径格式：{@code data/users/{userId}/files/original/{storedName}}
     * 使用 {@link Path#normalize()} + {@link Path#startsWith(Path)} 确保
     * storedName 中的 {@code ../} 无法逃逸出用户目录。
     */
    Path resolvePath(String userId, String storedName) {
        Path root = Paths.get(filesRoot, userId, ORIGINAL_DIR).normalize().toAbsolutePath();
        Path filePath = root.resolve(storedName).normalize();

        // 路径穿越防护：校验最终路径仍在用户目录内
        if (!filePath.startsWith(root)) {
            throw new SecurityException("非法路径访问: " + storedName);
        }

        return filePath;
    }

    /**
     * 提取文件扩展名（不含点）
     *
     * @param filename 原始文件名
     * @return 扩展名，如 "md"，无法识别返回空字符串
     */
    public String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase();
    }

    /**
     * 获取用户原始文件目录
     */
    Path getUserOriginalDir(String userId) {
        return Paths.get(filesRoot, userId, ORIGINAL_DIR).normalize().toAbsolutePath();
    }
}
