package org.digitalmind.pdf.qr.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PdfQRService {

    public StorageResource createPdfWithQRMetadata(
            String pdfQRSpecificationName, StorageResourceRef... storageResourceRefs
    ) throws IOException, HmacFieldException, HmacSignExcception;

    public StorageResource createPdfWithQRMetadata(
            PdfQRSpecification pdfQRSpecification, StorageResourceRef... storageResourceRefs
    ) throws IOException, HmacFieldException, HmacSignExcception;

    public PdfQRScanResult scanPdfWithQRMetadata(MultipartFile multipartFile, PdfQRSpecification.PdfQRPosition ... pdfQRPositions) throws IOException;

}
