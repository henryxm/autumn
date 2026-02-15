package cn.org.autumn.modules.db.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.db.dao.DatabaseBackupUploadDao;
import cn.org.autumn.modules.db.entity.DatabaseBackupUploadEntity;
import cn.org.autumn.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DatabaseBackupUploadService extends ModuleService<DatabaseBackupUploadDao, DatabaseBackupUploadEntity> {

    @Value("${autumn.backup.dir:#{systemProperties['user.home'] + '/backups'}}")
    private String backupDir;

    /**
     * 分片上传会话: uploadToken -> ChunkUploadSession
     */
    private final ConcurrentHashMap<String, ChunkUploadSession> uploadSessions = new ConcurrentHashMap<>();

    /**
     * 分页查询上传记录
     */
    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(params, "id");
    }

    /**
     * 上传SQL备份文件（小文件直传）
     */
    public DatabaseBackupUploadEntity uploadFile(MultipartFile file, String remark, String databaseName) throws IOException {
        Path uploadDir = Paths.get(backupDir, "uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        String originalFilename = file.getOriginalFilename();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String storedFilename = "upload_" + timestamp + "_" + sanitizeFilename(originalFilename);
        Path filePath = uploadDir.resolve(storedFilename);
        file.transferTo(filePath.toFile());
        DatabaseBackupUploadEntity entity = new DatabaseBackupUploadEntity();
        entity.setOriginalFilename(originalFilename);
        entity.setFilename(storedFilename);
        entity.setFilepath(filePath.toAbsolutePath().toString());
        entity.setFilesize(file.getSize());
        entity.setDatabase(databaseName);
        entity.setRemark(remark);
        entity.setStatus(0);
        entity.setCreateTime(new Date());
        save(entity);
        log.info("SQL backup file uploaded: originalName={}, storedName={}, size={}", originalFilename, storedFilename, file.getSize());
        return entity;
    }

    // ========================================
    // 分片上传
    // ========================================

    /**
     * 初始化分片上传，返回 uploadToken
     */
    public Map<String, Object> initChunkUpload(String originalFilename, long totalSize, int totalChunks, String remark, String databaseName) throws IOException {
        Path uploadDir = Paths.get(backupDir, "uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String storedFilename = "upload_" + timestamp + "_" + sanitizeFilename(originalFilename);
        String uploadToken = UUID.randomUUID().toString().replace("-", "");
        // 创建分片临时目录
        Path chunkDir = Paths.get(backupDir, "uploads", ".chunks_" + uploadToken);
        Files.createDirectories(chunkDir);
        ChunkUploadSession session = new ChunkUploadSession();
        session.uploadToken = uploadToken;
        session.originalFilename = originalFilename;
        session.storedFilename = storedFilename;
        session.targetPath = uploadDir.resolve(storedFilename).toAbsolutePath().toString();
        session.chunkDir = chunkDir.toAbsolutePath().toString();
        session.totalSize = totalSize;
        session.totalChunks = totalChunks;
        session.remark = remark;
        session.databaseName = databaseName;
        session.receivedChunks = Collections.synchronizedSet(new HashSet<>());
        session.createTime = System.currentTimeMillis();
        uploadSessions.put(uploadToken, session);
        Map<String, Object> result = new HashMap<>();
        result.put("uploadToken", uploadToken);
        result.put("totalChunks", totalChunks);
        log.info("Chunk upload initialized: token={}, file={}, size={}, chunks={}", uploadToken, originalFilename, totalSize, totalChunks);
        return result;
    }

    /**
     * 接收单个分片
     */
    public Map<String, Object> uploadChunk(String uploadToken, int chunkIndex, MultipartFile chunkFile) throws IOException {
        ChunkUploadSession session = uploadSessions.get(uploadToken);
        if (session == null) {
            throw new RuntimeException("上传会话不存在或已过期，请重新上传");
        }
        // 保存分片到临时目录
        Path chunkPath = Paths.get(session.chunkDir, "chunk_" + String.format("%06d", chunkIndex));
        chunkFile.transferTo(chunkPath.toFile());
        session.receivedChunks.add(chunkIndex);
        Map<String, Object> result = new HashMap<>();
        result.put("chunkIndex", chunkIndex);
        result.put("received", session.receivedChunks.size());
        result.put("total", session.totalChunks);
        result.put("completed", session.receivedChunks.size() >= session.totalChunks);
        if (log.isDebugEnabled()) {
            log.debug("Chunk received: token={}, chunk={}/{}", uploadToken, session.receivedChunks.size(), session.totalChunks);
        }
        return result;
    }

    /**
     * 合并所有分片，生成最终文件并创建数据库记录
     */
    public DatabaseBackupUploadEntity mergeChunks(String uploadToken) throws IOException {
        ChunkUploadSession session = uploadSessions.get(uploadToken);
        if (session == null) {
            throw new RuntimeException("上传会话不存在或已过期");
        }
        if (session.receivedChunks.size() < session.totalChunks) {
            throw new RuntimeException("分片未全部上传完成，已收到 " + session.receivedChunks.size() + "/" + session.totalChunks);
        }
        Path targetPath = Paths.get(session.targetPath);
        // 按顺序合并所有分片
        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
             FileChannel outChannel = fos.getChannel()) {
            for (int i = 0; i < session.totalChunks; i++) {
                Path chunkPath = Paths.get(session.chunkDir, "chunk_" + String.format("%06d", i));
                if (!Files.exists(chunkPath)) {
                    throw new RuntimeException("分片 " + i + " 缺失");
                }
                try (FileInputStream fis = new FileInputStream(chunkPath.toFile());
                     FileChannel inChannel = fis.getChannel()) {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }
            }
        }
        // 获取最终文件大小
        long fileSize = Files.size(targetPath);
        // 清理分片临时文件
        cleanupChunkDir(session.chunkDir);
        // 创建数据库记录
        DatabaseBackupUploadEntity entity = new DatabaseBackupUploadEntity();
        entity.setOriginalFilename(session.originalFilename);
        entity.setFilename(session.storedFilename);
        entity.setFilepath(targetPath.toAbsolutePath().toString());
        entity.setFilesize(fileSize);
        entity.setDatabase(session.databaseName);
        entity.setRemark(session.remark);
        entity.setStatus(0);
        entity.setCreateTime(new Date());
        save(entity);
        // 移除会话
        uploadSessions.remove(uploadToken);
        log.info("Chunk upload merged: token={}, file={}, size={}", uploadToken, session.originalFilename, fileSize);
        return entity;
    }

    /**
     * 取消分片上传，清理临时文件
     */
    public void cancelChunkUpload(String uploadToken) {
        ChunkUploadSession session = uploadSessions.remove(uploadToken);
        if (session != null) {
            cleanupChunkDir(session.chunkDir);
            log.info("Chunk upload cancelled: token={}", uploadToken);
        }
    }

    /**
     * 清理分片临时目录
     */
    private void cleanupChunkDir(String chunkDirPath) {
        try {
            Path chunkDir = Paths.get(chunkDirPath);
            if (Files.exists(chunkDir)) {
                Files.list(chunkDir).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
                Files.deleteIfExists(chunkDir);
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup chunk dir: {}", chunkDirPath, e);
        }
    }

    /**
     * 分片上传会话
     */
    static class ChunkUploadSession {
        String uploadToken;
        String originalFilename;
        String storedFilename;
        String targetPath;
        String chunkDir;
        long totalSize;
        int totalChunks;
        String remark;
        String databaseName;
        Set<Integer> receivedChunks;
        long createTime;
    }

    // ========================================
    // 文件管理
    // ========================================

    /**
     * 删除上传的备份（含文件）
     */
    public boolean deleteUpload(Long id) {
        DatabaseBackupUploadEntity entity = getById(id);
        if (entity == null) {
            return false;
        }
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            return false;
        }
        if (entity.getFilepath() != null) {
            try {
                Files.deleteIfExists(Paths.get(entity.getFilepath()));
            } catch (IOException e) {
                log.warn("Failed to delete uploaded file: {}", entity.getFilepath(), e);
            }
        }
        return removeById(id);
    }

    /**
     * 获取上传的备份文件
     */
    public File getUploadFile(Long id) {
        DatabaseBackupUploadEntity entity = getById(id);
        if (entity == null || entity.getFilepath() == null) {
            return null;
        }
        File file = new File(entity.getFilepath());
        if (!file.exists()) {
            return null;
        }
        return file;
    }

    /**
     * 清理文件名中的特殊字符
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown.sql";
        return filename.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }
}
