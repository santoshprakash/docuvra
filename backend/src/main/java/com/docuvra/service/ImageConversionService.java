package com.docuvra.service;

import com.docuvra.config.ConverterProperties;
import com.docuvra.dto.CommandResult;
import com.docuvra.exception.ConversionException;
import com.docuvra.exception.ConverterNotInstalledException;
import com.docuvra.util.CommandExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ImageConversionService {

    private final ConverterProperties converterProperties;
    private final CommandExecutor commandExecutor;
    private final DocuvraPathService pathService;

    public String detectImageMagickExecutable() {
        if (converterProperties.imagemagickPath() != null && !converterProperties.imagemagickPath().isBlank()) {
            return converterProperties.imagemagickPath();
        }

        List<String> candidates = pathService.isWindows() ? List.of("magick") : List.of("magick", "convert");
        return candidates.stream()
                .filter(this::isUsableCandidate)
                .findFirst()
                .orElseThrow(() -> new ConverterNotInstalledException(
                        "ImageMagick converter not found. Please install ImageMagick or configure DOCUVRA_IMAGEMAGICK_PATH."
                ));
    }

    public Path convertImageToPdf(Path inputFile, Path outputPdf) {
        String executable = detectImageMagickExecutable();
        try {
            Files.createDirectories(outputPdf.getParent());
            Files.deleteIfExists(outputPdf);
        } catch (Exception exception) {
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.", exception);
        }

        CommandResult result = commandExecutor.execute(
                List.of(executable, inputFile.toString(), "-auto-orient", outputPdf.toString()),
                timeout()
        );
        if (!result.success()) {
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.");
        }
        return outputPdf;
    }

    private Duration timeout() {
        long seconds = converterProperties.timeoutSeconds() <= 0 ? 120 : converterProperties.timeoutSeconds();
        return Duration.ofSeconds(seconds);
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
}
