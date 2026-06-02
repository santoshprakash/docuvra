package com.docuvra.service;

import com.docuvra.dto.CommandResult;
import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.entity.OcrPageEntity;
import com.docuvra.exception.ConversionException;
import com.docuvra.repository.OcrPageRepository;
import com.docuvra.util.CommandExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcrExtractionService {

    private static final Duration OCR_TIMEOUT = Duration.ofSeconds(180);

    private final DocumentService documentService;
    private final DocumentConversionService documentConversionService;
    private final FileStorageService fileStorageService;
    private final OcrPageRepository ocrPageRepository;
    private final CommandExecutor commandExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void ensureOcrText(UUID documentId, UUID versionId) {
        if (ocrPageRepository.existsByVersionId(versionId)) {
            return;
        }

        DocumentVersionEntity version = documentService.getVersion(documentId, versionId);
        Path originalFile = fileStorageService.restoreOriginalFileIfMissing(version);
        if (documentConversionService.isImageFile(version)) {
            extractImagePage(version, originalFile, 1);
            return;
        }

        if (documentConversionService.isPdf(version)) {
            extractPdfPages(version, originalFile);
            return;
        }

        throw new ConversionException("OCR is not available for this file type.");
    }

    private void extractPdfPages(DocumentVersionEntity version, Path pdfFile) {
        try (PDDocument document = Loader.loadPDF(pdfFile.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int index = 0; index < document.getNumberOfPages(); index += 1) {
                BufferedImage image = renderer.renderImageWithDPI(index, 150, ImageType.RGB);
                Path tempImage = Files.createTempFile("docuvra-ocr-page-", ".png");
                try {
                    ImageIO.write(image, "png", tempImage.toFile());
                    extractBufferedImage(version, tempImage, index + 1, image.getWidth(), image.getHeight());
                } finally {
                    Files.deleteIfExists(tempImage);
                }
            }
        } catch (Exception exception) {
            throw new ConversionException("Unable to scan this document with OCR.", exception);
        }
    }

    private void extractImagePage(DocumentVersionEntity version, Path imageFile, int pageNumber) {
        try {
            BufferedImage image = ImageIO.read(imageFile.toFile());
            if (image == null) {
                throw new ConversionException("Unable to read image for OCR.");
            }
            extractBufferedImage(version, imageFile, pageNumber, image.getWidth(), image.getHeight());
        } catch (ConversionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ConversionException("Unable to scan this image with OCR.", exception);
        }
    }

    private void extractBufferedImage(DocumentVersionEntity version, Path imageFile, int pageNumber, int imageWidth, int imageHeight) throws Exception {
        CommandResult result = commandExecutor.execute(
                List.of("tesseract", imageFile.toString(), "stdout", "--psm", "6", "tsv"),
                OCR_TIMEOUT
        );
        if (!result.success()) {
            throw new ConversionException("OCR scan failed. Please make sure Tesseract OCR is installed.");
        }

        OcrResult ocrResult = parseTsv(result.stdout(), imageWidth, imageHeight);
        OcrPageEntity page = OcrPageEntity.builder()
                .id(UUID.randomUUID())
                .document(version.getDocument())
                .version(version)
                .pageNumber(pageNumber)
                .extractedText(ocrResult.text())
                .boxesJson(objectMapper.writeValueAsString(ocrResult.boxes()))
                .extractedAt(LocalDateTime.now())
                .build();
        ocrPageRepository.saveAndFlush(page);
    }

    private OcrResult parseTsv(String tsv, int imageWidth, int imageHeight) {
        List<String> words = new ArrayList<>();
        List<Map<String, Object>> boxes = new ArrayList<>();
        String[] lines = tsv.split("\\R");
        for (int index = 1; index < lines.length; index += 1) {
            String[] columns = lines[index].split("\\t", -1);
            if (columns.length < 12 || !"5".equals(columns[0])) {
                continue;
            }

            String text = columns[11] == null ? "" : columns[11].trim();
            if (text.isBlank()) {
                continue;
            }

            double confidence = parseDouble(columns[10], -1);
            if (confidence < 0) {
                continue;
            }

            int left = parseInt(columns[6], 0);
            int top = parseInt(columns[7], 0);
            int width = parseInt(columns[8], 0);
            int height = parseInt(columns[9], 0);
            words.add(text);

            Map<String, Object> box = new LinkedHashMap<>();
            box.put("text", text);
            box.put("xPercent", percent(left, imageWidth));
            box.put("yPercent", percent(top, imageHeight));
            box.put("widthPercent", percent(width, imageWidth));
            box.put("heightPercent", percent(height, imageHeight));
            boxes.add(box);
        }

        return new OcrResult(String.join(" ", words), boxes);
    }

    private double percent(int value, int size) {
        return size <= 0 ? 0 : Math.max(0, Math.min(100, (value * 100.0) / size));
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.replace(',', '.').toLowerCase(Locale.ROOT));
        } catch (Exception exception) {
            return fallback;
        }
    }

    private record OcrResult(String text, List<Map<String, Object>> boxes) {
    }
}
