package org.digitalmind.pdf.qr.service;

import java.io.OutputStream;

public interface PdfQRFileReader {
    public OutputStream getFile(String url);
}
