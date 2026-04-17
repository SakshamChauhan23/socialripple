package com.social.ripple.external_ingestion.implementation;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PdfToImageService {


    private int dpi = 300;

    private String imageFormat = "png";

    /**
     * Convert PDF byte array to list of image byte arrays
     * @param pdfBytes PDF document as byte array
     * @return List of image byte arrays (one per page)
     */
    public List<byte[]> convertPdfToImagesBytes(byte[] pdfBytes) {
        List<byte[]> imageBytesList = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, dpi);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, imageFormat.toUpperCase(), baos);
                imageBytesList.add(baos.toByteArray());

                log.info("Converted page {} to {} image ({} bytes)",
                        page + 1, imageFormat, baos.size());
            }

            log.info("Successfully converted PDF with {} pages to {} images",
                    document.getNumberOfPages(), imageFormat);

        } catch (IOException e) {
            throw new RuntimeException("Error converting PDF bytes to images", e);
        }

        return imageBytesList;
    }

//    /**
//     * Convert PDF byte array to images with custom settings
//     * @param pdfBytes PDF document as byte array
//     * @param customDpi Custom DPI setting
//     * @param customFormat Custom image format (PNG, JPEG, etc.)
//     * @param maxPages Maximum number of pages to convert (null for all pages)
//     * @return List of image byte arrays
//     */
//    public List<byte[]> convertPdfToImagesBytesWithCustomSettings(byte[] pdfBytes,
//                                                                  Integer customDpi,
//                                                                  String customFormat,
//                                                                  Integer maxPages) {
//        List<byte[]> imageBytesList = new ArrayList<>();
//
//        try (PDDocument document = PDDocument.load(pdfBytes)) {
//            PDFRenderer pdfRenderer = new PDFRenderer(document);
//
//            int actualDpi = customDpi != null ? customDpi : dpi;
//            String actualFormat = customFormat != null ? customFormat : imageFormat;
//            int totalPages = document.getNumberOfPages();
//            int pagesToConvert = maxPages != null ? Math.min(maxPages, totalPages) : totalPages;
//
//            for (int page = 0; page < pagesToConvert; ++page) {
//                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, actualDpi);
//
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                ImageIO.write(bim, actualFormat.toUpperCase(), baos);
//                imageBytesList.add(baos.toByteArray());
//
//                logger.debug("Converted page {} to {} image with DPI {} ({} bytes)",
//                        page + 1, actualFormat, actualDpi, baos.size());
//            }
//
//            logger.info("Converted {} of {} pages to {} images with DPI {}",
//                    pagesToConvert, totalPages, actualFormat, actualDpi);
//
//        } catch (IOException e) {
//            throw new RuntimeException("Error converting PDF to images with custom settings", e);
//        }
//
//        return imageBytesList;
//    }
//
//    /**
//     * Convert PDF byte array to compressed JPEG images
//     * @param pdfBytes PDF document as byte array
//     * @param quality JPEG quality (0.0 - 1.0)
//     * @param maxWidth Maximum width for resizing (maintains aspect ratio)
//     * @return List of JPEG image byte arrays
//     */
//    public List<byte[]> convertPdfToCompressedJpegBytes(byte[] pdfBytes, float quality, Integer maxWidth) {
//        List<byte[]> imageBytesList = new ArrayList<>();
//
//        try (PDDocument document = PDDocument.load(pdfBytes)) {
//            PDFRenderer pdfRenderer = new PDFRenderer(document);
//
//            for (int page = 0; page < document.getNumberOfPages(); ++page) {
//                BufferedImage originalImage = pdfRenderer.renderImageWithDPI(page, dpi);
//                BufferedImage processedImage = originalImage;
//
//                // Resize if maxWidth is specified
//                if (maxWidth != null && maxWidth > 0 && originalImage.getWidth() > maxWidth) {
//                    processedImage = resizeImage(originalImage, maxWidth);
//                }
//
//                // Convert to JPEG with specified quality
//                byte[] jpegBytes = convertToJpegWithQuality(processedImage, quality);
//                imageBytesList.add(jpegBytes);
//
//                logger.debug("Converted page {} to JPEG (quality: {}, size: {} bytes)",
//                        page + 1, quality, jpegBytes.length);
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException("Error converting PDF to compressed JPEG images", e);
//        }
//
//        return imageBytesList;
//    }
//
//    /**
//     * Convert specific page of PDF to image byte array
//     * @param pdfBytes PDF document as byte array
//     * @param pageNumber Page number (1-based)
//     * @return Image byte array for the specified page
//     */
//    public byte[] convertPdfPageToImageBytes(byte[] pdfBytes, int pageNumber) {
//        return convertPdfPageToImageBytes(pdfBytes, pageNumber, dpi, imageFormat);
//    }
//
//    /**
//     * Convert specific page of PDF to image byte array with custom settings
//     * @param pdfBytes PDF document as byte array
//     * @param pageNumber Page number (1-based)
//     * @param customDpi Custom DPI setting
//     * @param customFormat Custom image format
//     * @return Image byte array for the specified page
//     */
//    public byte[] convertPdfPageToImageBytes(byte[] pdfBytes, int pageNumber,
//                                             Integer customDpi, String customFormat) {
//        try (PDDocument document = PDDocument.load(pdfBytes)) {
//            if (pageNumber < 1 || pageNumber > document.getNumberOfPages()) {
//                throw new IllegalArgumentException(
//                        String.format("Page number %d is out of range. Document has %d pages.",
//                                pageNumber, document.getNumberOfPages()));
//            }
//
//            PDFRenderer pdfRenderer = new PDFRenderer(document);
//            int actualDpi = customDpi != null ? customDpi : dpi;
//            String actualFormat = customFormat != null ? customFormat : imageFormat;
//
//            BufferedImage bim = pdfRenderer.renderImageWithDPI(pageNumber - 1, actualDpi);
//
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            ImageIO.write(bim, actualFormat.toUpperCase(), baos);
//
//            logger.info("Converted page {} to {} image with DPI {} ({} bytes)",
//                    pageNumber, actualFormat, actualDpi, baos.size());
//
//            return baos.toByteArray();
//
//        } catch (IOException e) {
//            throw new RuntimeException("Error converting PDF page to image", e);
//        }
//    }
//
//    /**
//     * Get PDF metadata without converting to images
//     * @param pdfBytes PDF document as byte array
//     * @return PDF metadata
//     */
//    public PdfMetadata getPdfMetadata(byte[] pdfBytes) {
//        try (PDDocument document = PDDocument.load(pdfBytes)) {
//            PdfMetadata metadata = new PdfMetadata();
//            metadata.setPageCount(document.getNumberOfPages());
//            metadata.setEncrypted(document.isEncrypted());
//
//            PDDocumentInformation docInfo = document.getDocumentInformation();
//            if (docInfo != null) {
//                metadata.setTitle(docInfo.getTitle());
//                metadata.setAuthor(docInfo.getAuthor());
//                metadata.setSubject(docInfo.getSubject());
//                metadata.setKeywords(docInfo.getKeywords());
//                metadata.setCreator(docInfo.getCreator());
//                metadata.setProducer(docInfo.getProducer());
//
//                if (docInfo.getCreationDate() != null) {
//                    metadata.setCreationDate(docInfo.getCreationDate().getTime());
//                }
//                if (docInfo.getModificationDate() != null) {
//                    metadata.setModificationDate(docInfo.getModificationDate().getTime());
//                }
//            }
//
//            return metadata;
//
//        } catch (IOException e) {
//            throw new RuntimeException("Error reading PDF metadata", e);
//        }
//    }
//
//    /**
//     * Resize image while maintaining aspect ratio
//     */
//    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth) {
//        int originalWidth = originalImage.getWidth();
//        int originalHeight = originalImage.getHeight();
//
//        if (originalWidth <= maxWidth) {
//            return originalImage;
//        }
//
//        int newWidth = maxWidth;
//        int newHeight = (originalHeight * maxWidth) / originalWidth;
//
//        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
//        Graphics2D g = resizedImage.createGraphics();
//
//        try {
//            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//            g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
//        } finally {
//            g.dispose();
//        }
//
//        return resizedImage;
//    }
//
//    /**
//     * Convert BufferedImage to JPEG with specified quality
//     */
//    private byte[] convertToJpegWithQuality(BufferedImage image, float quality) throws IOException {
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG");
//            if (!writers.hasNext()) {
//                throw new IllegalStateException("No JPEG ImageWriter found");
//            }
//
//            ImageWriter writer = writers.next();
//            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
//                writer.setOutput(ios);
//
//                ImageWriteParam param = writer.getDefaultWriteParam();
//                if (param.canWriteCompressed()) {
//                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//                    param.setCompressionQuality(quality);
//                }
//
//                writer.write(null, new IIOImage(image, null, null), param);
//            } finally {
//                writer.dispose();
//            }
//
//            return baos.toByteArray();
//        }
//    }
//
//    // PDF Metadata inner class
//    public static class PdfMetadata {
//        private int pageCount;
//        private boolean encrypted;
//        private String title;
//        private String author;
//        private String subject;
//        private String keywords;
//        private String creator;
//        private String producer;
//        private Long creationDate;
//        private Long modificationDate;
//
//        // Getters and setters
//        public int getPageCount() { return pageCount; }
//        public void setPageCount(int pageCount) { this.pageCount = pageCount; }
//
//        public boolean isEncrypted() { return encrypted; }
//        public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
//
//        public String getTitle() { return title; }
//        public void setTitle(String title) { this.title = title; }
//
//        public String getAuthor() { return author; }
//        public void setAuthor(String author) { this.author = author; }
//
//        public String getSubject() { return subject; }
//        public void setSubject(String subject) { this.subject = subject; }
//
//        public String getKeywords() { return keywords; }
//        public void setKeywords(String keywords) { this.keywords = keywords; }
//
//        public String getCreator() { return creator; }
//        public void setCreator(String creator) { this.creator = creator; }
//
//        public String getProducer() { return producer; }
//        public void setProducer(String producer) { this.producer = producer; }
//
//        public Long getCreationDate() { return creationDate; }
//        public void setCreationDate(Long creationDate) { this.creationDate = creationDate; }
//
//        public Long getModificationDate() { return modificationDate; }
//        public void setModificationDate(Long modificationDate) { this.modificationDate = modificationDate; }
//
//        @Override
//        public String toString() {
//            return String.format("PdfMetadata{pages=%d, encrypted=%s, title='%s', author='%s'}",
//                    pageCount, encrypted, title, author);
//        }
//    }
}
