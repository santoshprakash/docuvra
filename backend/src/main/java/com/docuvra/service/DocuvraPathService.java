package com.docuvra.service;

import com.docuvra.config.StorageProperties;
import com.docuvra.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocuvraPathService {

    private final StorageProperties storageProperties;

    public boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public Path getUploadBasePath() {
        return configuredOrDefault(storageProperties.basePath(), isWindows() ? "C:/docuvra/uploads" : "/opt/docuvra/uploads");
    }

    public Path getConvertedBasePath() {
        return configuredOrDefault(storageProperties.convertedPath(), isWindows() ? "C:/docuvra/converted" : "/opt/docuvra/converted");
    }

    public Path resolveUploadPath(UUID documentId, UUID versionId, String fileName) {
        Path versionFolder = resolveUploadVersionFolder(documentId, versionId);
        Path path = versionFolder.resolve(fileName).normalize();
        ensurePathInside(versionFolder, path);
        return path;
    }

    public Path resolveConvertedPdfPath(UUID documentId, UUID versionId) {
        Path versionFolder = resolveConvertedVersionFolder(documentId, versionId);
        Path path = versionFolder.resolve("viewer.pdf").normalize();
        ensurePathInside(versionFolder, path);
        return path;
    }

    public Path resolveConvertedVersionFolder(UUID documentId, UUID versionId) {
        Path folder = getConvertedBasePath()
                .resolve(documentId.toString())
                .resolve(versionId.toString())
                .normalize();
        ensurePathInside(getConvertedBasePath(), folder);
        return folder;
    }

    public Path resolveConvertedDocumentFolder(UUID documentId) {
        Path folder = getConvertedBasePath().resolve(documentId.toString()).normalize();
        ensurePathInside(getConvertedBasePath(), folder);
        return folder;
    }

    public Path resolveUploadVersionFolder(UUID documentId, UUID versionId) {
        Path folder = getUploadBasePath()
                .resolve(documentId.toString())
                .resolve(versionId.toString())
                .normalize();
        ensurePathInside(getUploadBasePath(), folder);
        return folder;
    }

    public void ensureDirectories() {
        try {
            Files.createDirectories(getUploadBasePath());
            Files.createDirectories(getConvertedBasePath());
        } catch (IOException exception) {
            throw new FileStorageException("Failed to create Docuvra storage folders.", exception);
        }
    }

    public void ensurePathInside(Path parent, Path child) {
        Path normalizedParent = parent.toAbsolutePath().normalize();
        Path normalizedChild = child.toAbsolutePath().normalize();

        if (!normalizedChild.startsWith(normalizedParent)) {
            throw new FileStorageException("Invalid file path.");
        }
    }

    private Path configuredOrDefault(String configuredPath, String defaultPath) {
        String path = configuredPath == null || configuredPath.isBlank() ? defaultPath : configuredPath;
        return Path.of(path).toAbsolutePath().normalize();
    }
}
