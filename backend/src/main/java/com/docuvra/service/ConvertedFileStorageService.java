package com.docuvra.service;

import com.docuvra.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConvertedFileStorageService {

    private final DocuvraPathService pathService;

    public Path getConvertedPdfPath(UUID documentId, UUID versionId) {
        return pathService.resolveConvertedPdfPath(documentId, versionId);
    }

    public boolean convertedPdfExists(UUID documentId, UUID versionId) {
        Path path = getConvertedPdfPath(documentId, versionId);
        try {
            return Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (IOException exception) {
            return false;
        }
    }

    public Path ensureConvertedFolder(UUID documentId, UUID versionId) {
        Path folder = pathService.resolveConvertedVersionFolder(documentId, versionId);
        try {
            Files.createDirectories(folder);
            return folder;
        } catch (IOException exception) {
            throw new FileStorageException("Failed to create converted document folder.", exception);
        }
    }

    public void deleteConvertedFile(UUID documentId, UUID versionId) {
        try {
            Files.deleteIfExists(getConvertedPdfPath(documentId, versionId));
        } catch (IOException exception) {
            throw new FileStorageException("Failed to delete converted PDF.", exception);
        }
    }

    public void deleteConvertedDocumentFolder(UUID documentId) {
        deleteFolder(pathService.resolveConvertedDocumentFolder(documentId));
    }

    public void deleteAllConvertedFiles() {
        deleteFolder(pathService.getConvertedBasePath());
        pathService.ensureDirectories();
    }

    public InputStream streamConvertedPdf(UUID documentId, UUID versionId) {
        try {
            return new FileInputStream(getConvertedPdfPath(documentId, versionId).toFile());
        } catch (IOException exception) {
            throw new FileStorageException("Failed to open converted PDF stream.", exception);
        }
    }

    private void deleteFolder(Path folder) {
        if (!Files.exists(folder)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deletePath);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to delete converted files.", exception);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to delete path: " + path, exception);
        }
    }
}
