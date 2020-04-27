package org.digitalmind.pdf.qr.dto;

import lombok.Getter;

@Getter
public enum PdfQRScanPageStatus {
    SUCCESS(00, "Success", true),
    PAGE_PROCESS_ERROR(10, "Error  processing document page", false),
    QR_NOT_FOUND_ERROR(20, "QR code is missing or not detected from a page", false),
    QR_FORMAT_ERROR(30, "QR code has a wrong format", false);

    private int code;
    private String description;
    private boolean success;

    PdfQRScanPageStatus(int code, String description, boolean success) {
        this.code = code;
        this.description = description;
        this.success = success;
    }
}
