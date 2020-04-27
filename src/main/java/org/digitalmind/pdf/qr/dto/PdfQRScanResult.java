package org.digitalmind.pdf.qr.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.core.io.Resource;

import java.util.Map;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Data
public class PdfQRScanResult {
    private boolean success;
    @Singular
    private Map<StorageResourceRef, PdfQRScanDocumentResult> documents;
    @Singular
    private Map<Integer, PdfQRScanPageResult> pages;

    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    @Data
    public static class PdfQRScanDocumentResult {
        private Resource resource;
        private PdfQRScanDocumentStatus status;
        private String description;
    }

    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    @Data
    public static class PdfQRScanPageResult {
        private int pageNumber;
        private String qrCode;
        private PdfQRScanPageStatus status;
        private String description;
    }

}
