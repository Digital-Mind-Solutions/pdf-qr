package org.digitalmind.pdf.qr.dto;

import lombok.Getter;

@Getter
public enum PdfQRScanDocumentStatus {
    SUCCESS(00, "Success", true),
    ERROR(10, "Error  processing document", false),
    NOT_FOUND(20, "Document not found in storage", false),
    PAGE_MISSING(30, "Pages are missing from scanned document", false),
    QR_NOT_FOUND(40, "QR code is missing or not detected from a page", false);

    private int code;
    private String description;
    private boolean success;

    PdfQRScanDocumentStatus(int code, String description, boolean success) {
        this.code = code;
        this.description = description;
        this.success = success;
    }
}
