package com.docuvra.service;

import com.docuvra.dto.ThumbnailResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

@Slf4j
@Service
public class ThumbnailService {

    private static final int THUMBNAIL_WIDTH = 240;
    private static final int PLACEHOLDER_HEIGHT = 320;
    private static final String PNG_MIME_TYPE = "image/png";

    public ThumbnailResult generateThumbnail(MultipartFile file, String mimeType, Path savedFilePath) {
        try {
            if (isPdf(file, mimeType)) {
                return generatePdfThumbnail(savedFilePath);
            }
        } catch (Exception exception) {
            log.warn("Failed to generate PDF thumbnail for fileName={}", file.getOriginalFilename(), exception);
        }

        try {
            return generatePlaceholderThumbnail(mimeType, file.getOriginalFilename());
        } catch (Exception exception) {
            log.warn("Failed to generate placeholder thumbnail for fileName={}", file.getOriginalFilename(), exception);
            return new ThumbnailResult(new byte[0], PNG_MIME_TYPE);
        }
    }

    public ThumbnailResult generatePdfThumbnail(Path pdfPath) {
        return generateThumbnailFromPdf(pdfPath);
    }

    public ThumbnailResult generateThumbnailFromPdf(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (document.getNumberOfPages() == 0) {
                throw new IOException("PDF has no pages.");
            }

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage firstPage = renderer.renderImageWithDPI(0, 120, ImageType.RGB);
            BufferedImage scaledImage = scaleToWidth(firstPage, THUMBNAIL_WIDTH);
            return new ThumbnailResult(toPngBytes(scaledImage), PNG_MIME_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate PDF thumbnail.", exception);
        }
    }

    public ThumbnailResult generatePlaceholderThumbnail(String mimeType, String fileName) {
        try {
            BufferedImage image = new BufferedImage(THUMBNAIL_WIDTH, PLACEHOLDER_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setColor(new Color(248, 250, 252));
                graphics.fillRect(0, 0, THUMBNAIL_WIDTH, PLACEHOLDER_HEIGHT);
                graphics.setColor(new Color(226, 232, 240));
                graphics.fillRoundRect(26, 24, THUMBNAIL_WIDTH - 52, PLACEHOLDER_HEIGHT - 48, 18, 18);
                graphics.setColor(new Color(255, 255, 255));
                graphics.fillRoundRect(40, 40, THUMBNAIL_WIDTH - 80, PLACEHOLDER_HEIGHT - 80, 12, 12);
                graphics.setColor(new Color(148, 163, 184));
                graphics.setStroke(new BasicStroke(3));
                graphics.drawRoundRect(40, 40, THUMBNAIL_WIDTH - 80, PLACEHOLDER_HEIGHT - 80, 12, 12);

                String extension = extensionLabel(mimeType, fileName);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fitFontSize(graphics, extension)));
                graphics.setColor(new Color(37, 99, 235));
                FontMetrics metrics = graphics.getFontMetrics();
                int textX = (THUMBNAIL_WIDTH - metrics.stringWidth(extension)) / 2;
                int textY = (PLACEHOLDER_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
                graphics.drawString(extension, textX, textY);
            } finally {
                graphics.dispose();
            }

            return new ThumbnailResult(toPngBytes(image), PNG_MIME_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate placeholder thumbnail.", exception);
        }
    }

    private boolean isPdf(MultipartFile file, String mimeType) {
        String fileName = file.getOriginalFilename();
        return "application/pdf".equalsIgnoreCase(mimeType)
                || (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"));
    }

    private BufferedImage scaleToWidth(BufferedImage source, int targetWidth) {
        int targetHeight = Math.max(1, Math.round((float) source.getHeight() * targetWidth / source.getWidth()));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private byte[] toPngBytes(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }

    private int fitFontSize(Graphics2D graphics, String text) {
        int fontSize = 46;
        while (fontSize > 22) {
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
            if (graphics.getFontMetrics(font).stringWidth(text) <= THUMBNAIL_WIDTH - 72) {
                return fontSize;
            }
            fontSize -= 2;
        }
        return fontSize;
    }

    private String extensionLabel(String mimeType, String fileName) {
        String extension = extensionFromFileName(fileName);
        if (!extension.isBlank()) {
            return extension;
        }

        if (mimeType == null || mimeType.isBlank()) {
            return "FILE";
        }

        return switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "application/pdf" -> "PDF";
            case "text/plain" -> "TXT";
            case "image/jpeg" -> "JPG";
            case "image/png" -> "PNG";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "DOCX";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "XLSX";
            default -> "FILE";
        };
    }

    private String extensionFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }

        String extension = fileName.substring(extensionIndex + 1).trim().toUpperCase(Locale.ROOT);
        if (extension.length() > 6) {
            return "FILE";
        }
        return extension;
    }
}
