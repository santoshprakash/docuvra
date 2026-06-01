package com.docuvra.service;

import com.docuvra.config.ConverterProperties;
import com.docuvra.dto.CommandResult;
import com.docuvra.exception.ConversionException;
import com.docuvra.exception.ConverterNotInstalledException;
import com.docuvra.util.CommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OfficeConverterService {

    private final ConverterProperties converterProperties;
    private final CommandExecutor commandExecutor;
    private final DocuvraPathService pathService;

    public String detectOfficeExecutable() {
        if (converterProperties.officePath() != null && !converterProperties.officePath().isBlank()) {
            return converterProperties.officePath();
        }

        List<String> candidates = pathService.isWindows()
                ? List.of(
                "C:/Program Files/LibreOffice/program/soffice.exe",
                "C:/Program Files/OpenOffice 4/program/soffice.exe",
                "soffice",
                "libreoffice",
                "openoffice"
        )
                : List.of("soffice", "libreoffice", "openoffice");

        return candidates.stream()
                .filter(this::isUsableCandidate)
                .findFirst()
                .orElseThrow(() -> new ConverterNotInstalledException(
                        "OpenOffice/LibreOffice converter not found. Please install OpenOffice or LibreOffice, or configure DOCUVRA_OFFICE_PATH."
                ));
    }

    public Path convert(Path inputFile, Path outputDir, Path finalPdfPath) {
        String executable = detectOfficeExecutable();
        try {
            Files.createDirectories(outputDir);
            Files.deleteIfExists(finalPdfPath);
        } catch (IOException exception) {
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.", exception);
        }

        List<String> command = List.of(
                executable,
                "--headless",
                "--convert-to",
                "pdf",
                "--outdir",
                outputDir.toString(),
                inputFile.toString()
        );
        CommandResult result = commandExecutor.execute(command, timeout());
        if (!result.success()) {
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.");
        }

        Path generatedPdf = locateGeneratedPdf(inputFile, outputDir);
        try {
            Files.move(generatedPdf, finalPdfPath, StandardCopyOption.REPLACE_EXISTING);
            return finalPdfPath;
        } catch (IOException exception) {
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.", exception);
        }
    }

    private Path locateGeneratedPdf(Path inputFile, Path outputDir) {
        String fileName = inputFile.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        Path expected = outputDir.resolve(baseName + ".pdf");
        if (Files.isRegularFile(expected)) {
            return expected;
        }

        try (var paths = Files.list(outputDir)) {
            return paths
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .findFirst()
                    .orElseThrow(() -> new ConversionException("Document preview conversion failed. Please try again or download the original file."));
        } catch (IOException exception) {
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.", exception);
        }
    }

    private boolean isUsableCandidate(String candidate) {
        Path path = Path.of(candidate);
        if (candidate.contains("/") || candidate.contains("\\")) {
            return Files.isRegularFile(path);
        }

        try {
            Process process = new ProcessBuilder(candidate, "--version").start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private Duration timeout() {
        long seconds = converterProperties.timeoutSeconds() <= 0 ? 120 : converterProperties.timeoutSeconds();
        return Duration.ofSeconds(seconds);
    }
}
