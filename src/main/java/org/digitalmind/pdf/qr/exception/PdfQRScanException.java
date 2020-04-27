package org.digitalmind.pdf.qr.exception;

public class PdfQRScanException extends Exception {

    public PdfQRScanException() {
    }

    public PdfQRScanException(String message) {
        super(message);
    }

    public PdfQRScanException(String message, Throwable cause) {
        super(message, cause);
    }

    public PdfQRScanException(Throwable cause) {
        super(cause);
    }

    public PdfQRScanException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
