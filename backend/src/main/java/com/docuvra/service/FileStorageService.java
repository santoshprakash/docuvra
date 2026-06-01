package com.docuvra.service;

import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final String DEFAULT_FILE_NAME = "uploaded-file";

    private final DocuvraPathService pathService;

    public Path saveFile(UUID documentId, UUID versionId, MultipartFile file) {
        String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
        Path versionFolder = pathService.resolveUploadVersionFolder(documentId, versionId);
        Path targetPath = pathService.resolveUploadPath(documentId, versionId, sanitizedFileName);

        pathService.ensurePathInside(versionFolder, targetPath);

        try {
            Files.createDirectories(versionFolder);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return targetPath;
        } catch (IOException exception) {
            throw new FileStorageException("Failed to save file to local storage.", exception);
        }
    }

    public InputStream getFileStream(DocumentVersionEntity version) {
        Path localPath = getOriginalFilePath(version);

        try {
            if (Files.exists(localPath) && Files.isRegularFile(localPath)) {
                return new FileInputStream(localPath.toFile());
            }

            Path restoredPath = restoreFileFromDatabase(version);
            return new FileInputStream(restoredPath.toFile());
        } catch (IOException exception) {
            throw new FileStorageException("Failed to open file stream.", exception);
        }
    }

    public Path getOriginalFilePath(DocumentVersionEntity version) {
        return Path.of(version.getLocalFilePath()).toAbsolutePath().normalize();
    }

    public Path restoreOriginalFileIfMissing(DocumentVersionEntity version) {
        Path localPath = getOriginalFilePath(version);
        if (Files.exists(localPath) && Files.isRegularFile(localPath)) {
            return localPath;
        }
        return restoreFileFromDatabase(version);
    }

    public Path restoreFileFromDatabase(DocumentVersionEntity version) {
        if (version.getFileData() == null || version.getFileData().length == 0) {
            throw new FileStorageException("File backup data is missing.");
        }

        UUID documentId = version.getDocument().getId();
        UUID versionId = version.getId();
        String sanitizedFileName = sanitizeFileName(version.getOriginalFileName());
        Path versionFolder = pathService.resolveUploadVersionFolder(documentId, versionId);
        Path restoredPath = pathService.resolveUploadPath(documentId, versionId, sanitizedFileName);

        pathService.ensurePathInside(versionFolder, restoredPath);

        try {
            Files.createDirectories(versionFolder);
            Files.write(restoredPath, version.getFileData());
            return restoredPath;
        } catch (IOException exception) {
            throw new FileStorageException("Failed to restore file from database backup.", exception);
        }
    }

    public void deleteVersionFile(DocumentVersionEntity version) {
        Path localPath = getOriginalFilePath(version);

        try {
            if (Files.exists(localPath)) {
                Files.delete(localPath);
            }
        } catch (IOException exception) {
            throw new FileStorageException("Failed to delete version file.", exception);
        }
    }

    public void deleteDocumentFolder(UUID documentId) {
        Path documentFolder = pathService.getUploadBasePath().resolve(documentId.toString()).normalize();
        pathService.ensurePathInside(pathService.getUploadBasePath(), documentFolder);

        if (!Files.exists(documentFolder)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(documentFolder)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(this::deletePath);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to delete document folder.", exception);
        }
    }

    public String sanitizeFileName(String fileName) {
        String candidate = fileName == null ? DEFAULT_FILE_NAME : fileName;
        String baseName = Path.of(candidate).getFileName().toString();
        String sanitized = baseName
                .replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_")
                .trim();

        if (sanitized.isBlank() || sanitized.equals(".") || sanitized.equals("..")) {
            return DEFAULT_FILE_NAME;
        }

        return sanitized;
    }

    public boolean isPdf(DocumentVersionEntity version) {
        String mimeType = version.getMimeType();
        String fileName = version.getOriginalFileName();

        return (mimeType != null && mimeType.equalsIgnoreCase("application/pdf"))
                || (fileName != null && fileName.toLowerCase().endsWith(".pdf"));
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to delete path: " + path, exception);
        }
    }
}
