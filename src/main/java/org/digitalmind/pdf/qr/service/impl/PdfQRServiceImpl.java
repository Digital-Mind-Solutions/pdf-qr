package org.digitalmind.pdf.qr.service.impl;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.digitalmind.barcode.service.BarcodeService;
import org.digitalmind.buildingblocks.security.hmac.exception.HmacFieldException;
import org.digitalmind.buildingblocks.security.hmac.exception.HmacSignExcception;
import org.digitalmind.buildingblocks.security.hmac.service.HmacService;
import org.digitalmind.pdf.qr.config.PdfQRConfig;
import org.digitalmind.pdf.qr.dto.PdfQRSpecification;
import org.digitalmind.pdf.qr.service.PdfQRService;
import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.digitalmind.pdf.qr.config.PdfQRModuleConfig.ENABLED;

@Service
@ConditionalOnProperty(name = ENABLED, havingValue = "true")
@Slf4j
public class PdfQRServiceImpl implements PdfQRService {

    private final BarcodeService barcodeService;
    private final HmacService hmacService;
    private final PdfQRConfig config;
    private final StrongTextEncryptor textEncryptor;

    @Autowired
    public PdfQRServiceImpl(
            BarcodeService barcodeService,
            HmacService hmacService,
            PdfQRConfig config) {
        this.barcodeService = barcodeService;
        this.hmacService = hmacService;
        this.config = config;
        this.textEncryptor = new StrongTextEncryptor();
        textEncryptor.setPassword("eSignature");
    }

    public StorageResource createPdfWithQRMetadata(String pdfQRSpecificationName, StorageResourceRef... storageResourceRefs)
            throws IOException, HmacFieldException, HmacSignExcception {
        PdfQRSpecification pdfQRSpecification = this.config.getConfig().getSpecifications().get(pdfQRSpecificationName);
        return createPdfWithQRMetadata(pdfQRSpecification, storageResourceRefs);
    }

    public StorageResource createPdfWithQRMetadata(PdfQRSpecification pdfQRSpecification, StorageResourceRef... storageResourceRefs)
            throws IOException, HmacFieldException, HmacSignExcception {
        StorageResource.StorageResourceBuilder storageResourceBuilder = StorageResource.builder();


        try (ByteArrayInOutStream pdfOutputStream = new ByteArrayInOutStream()) {
            String contentType = null;
            String contentDisposition = null;

            Document document = null;
            try {
                document = new Document();
                PdfCopy pdfCopy = new PdfCopy(document, pdfOutputStream);
                document.open();

                //get one by one the documents to be enhanced with QR
                for (StorageResourceRef storageResourceRef : storageResourceRefs) {
                    StorageResource storageResourceSource = storageService.download(storageResourceRef.getNamespace(), storageResourceRef.getName());
                    //contentType = storageResourceSource.getInfo().getContentType();
                    //contentDisposition = storageResourceSource.getInfo().getContentDisposition();

                    try (ByteArrayInOutStream pdfOutputStreamSource = new ByteArrayInOutStream()) {

                        //prepare the pdfOutputStreamSource
                        PdfStamper pdfStamper = null;
                        try (InputStream inputStreamSource = storageResourceSource.getResource().getInputStream()) {
                            PdfReader pdfReader = new PdfReader(inputStreamSource);
                            pdfStamper = new PdfStamper(pdfReader, pdfOutputStreamSource);
                            int intPages = pdfReader.getNumberOfPages();
                            Date date = new Date();
                            for (int intPage = 1; intPage <= intPages; intPage++) {
                                Map<String, Object> fields = new HashMap<>();
                                fields.put("namespace", storageResourceRef.getNamespace().getName());
                                fields.put("name", storageResourceRef.getName());
                                fields.put("page", intPage);
                                hmacService.addTemporalMark(
                                        fields,
                                        date,
                                        pdfQRSpecification.getValidity().getYears(),
                                        pdfQRSpecification.getValidity().getMonths(),
                                        pdfQRSpecification.getValidity().getDays(),
                                        pdfQRSpecification.getValidity().getHours(),
                                        pdfQRSpecification.getValidity().getMinutes(),
                                        pdfQRSpecification.getValidity().getSeconds()
                                );
                                String qrContent = hmacService.calculateUrl(
                                        config.getConfig().getHmac().getContext(),
                                        fields, new HashSet<>(),
                                        config.getConfig().getHmac().getBase(),
                                        config.getConfig().getHmac().getFragment()
                                );

                                String qrContentEncrypted = textEncryptor.encrypt(qrContent);
                                BarcodeRequest barcodeRequestPage = BarcodeRequest.builder()
                                        .content(qrContentEncrypted)
                                        .format(pdfQRSpecification.getFormat())
                                        .height(pdfQRSpecification.getHeight())
                                        .width(pdfQRSpecification.getWidth())
                                        .imageType(pdfQRSpecification.getImageType())
                                        .logo(pdfQRSpecification.getLogo())
                                        .onColor(pdfQRSpecification.getOnColor())
                                        .offColor(pdfQRSpecification.getOffColor())
                                        .hintTypes(pdfQRSpecification.getHintTypes())
                                        .build();
                                BarcodeResponse barcodeResponse = barcodeService.generate(barcodeRequestPage);
                                Image qrCodeImage = Image.getInstance(IOUtils.toByteArray(barcodeResponse.getResource().getInputStream()));

                                Rectangle pageSize = pdfReader.getPageSize(intPage);
                                float pageWidthA4 = PageSize.A4.getWidth();
                                float pageHeighthA4 = PageSize.A4.getHeight();
                                float dX = 0;
                                float dY = 0;
                                float sX = pageSize.getWidth() / pageWidthA4;
                                float sY = pageSize.getHeight() / pageHeighthA4;
                                sX = 1;
                                sY = 1;

                                if (PdfQRHAlign.LEFT.equals(pdfQRSpecification.getPosition().getHAlign())) {
                                    dX = pageSize.getLeft(pdfQRSpecification.getPosition().getHMargin());
                                } else if (PdfQRHAlign.RIGHT.equals(pdfQRSpecification.getPosition().getHAlign())) {
                                    dX = pageSize.getRight(pdfQRSpecification.getPosition().getHMargin());
                                }
                                if (PdfQRVAlign.TOP.equals(pdfQRSpecification.getPosition().getVAlign())) {
                                    dY = pageSize.getTop(pdfQRSpecification.getPosition().getVMargin());
                                } else if (PdfQRVAlign.BOTTOM.equals(pdfQRSpecification.getPosition().getVAlign())) {
                                    dY = pageSize.getBottom(pdfQRSpecification.getPosition().getVMargin());
                                }

                                //BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                                //PdfContentByte under, over;
                                //pdfStamper.setRotateContents(false);

                                PdfContentByte pdfPageContentByte = pdfStamper.getOverContent(intPage);

                                qrCodeImage.setBorder(1);
                                qrCodeImage.setAbsolutePosition(dX, dY);
                                qrCodeImage.scalePercent(sX * 100F);
                                //qrCodeImage.scalePercent(sX * 100F, sY * 100F);
                                pdfPageContentByte.addImage(qrCodeImage);
                            }
                        } finally {
                            if (pdfStamper != null) {
                                pdfStamper.close();
                                pdfStamper = null;
                            }
                        }
                        //pdfStamper is closed
                        //inputStreamSource is closed

                        //copy the pdfOutputStreamSource into the merged document
                        try (PdfReader pdfReader = new PdfReader(pdfOutputStreamSource.getInputStream())) {
                            // loop over the pages in that document
                            int intPages = pdfReader.getNumberOfPages();
                            for (int intPage = 1; intPage <= intPages; intPage++) {
                                pdfCopy.addPage(pdfCopy.getImportedPage(pdfReader, intPage));
                            }
                            pdfCopy.freeReader(pdfReader);
                        }
                        //pdfReader is closed

                    }
                    //pdfOutputStreamSource is closed
                }
            } finally {
                if (document != null && document.isOpen()) {
                    document.close();
                }
                document = null;
            }
            //document is closed

            contentType = "application/pdf";
            contentDisposition = "attachment; filename=" + UUID.randomUUID() + ".pdf";
            Resource resource = new ByteArrayResource(IOUtils.toByteArray(pdfOutputStream.getInputStream()));
            storageResourceBuilder
                    .ref(null)
                    .info(
                            StorageResourceContentInfo.builder()
                                    .contentDisposition(contentDisposition)
                                    .contentType(contentType)
                                    .length(resource.contentLength())
                                    .build()
                    )
                    .resource(resource);
        }
        //pdfOutputStream is closed

        return storageResourceBuilder.build();
    }

    @Override
    public PdfQRScanResult scanPdfWithQRMetadata(MultipartFile multipartFile, PdfQRSpecification.PdfQRPosition... pdfQRPositions) throws IOException {
        PdfQRScanResult.PdfQRScanResultBuilder pdfQRScanResultBuilder = PdfQRScanResult.builder().success(false);

        try (PDDocument pdfDocument = PDDocument.load(multipartFile.getInputStream())) {
            Splitter splitter = new Splitter();
            List<PDDocument> pdfDocumentPages = splitter.split(pdfDocument);
            Map<String, PDDocument> pdfDocumentList = new HashMap<>();

            PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
            for (int pageNumber = 1; pageNumber < pdfDocument.getNumberOfPages(); pageNumber++) {
                PdfQRScanResult.PdfQRScanPageResult.PdfQRScanPageResultBuilder pdfQRScanPageResultBuilder = PdfQRScanResult.PdfQRScanPageResult.builder();
                try {
                    String qrContentEncrypted = scanQRCode(pdfRenderer, pageNumber);
                    if (StringUtils.isEmpty(qrContentEncrypted)) {
                        pdfQRScanPageResultBuilder.status(PdfQRScanPageStatus.QR_NOT_FOUND_ERROR);
                        pdfQRScanPageResultBuilder.description("QR code not detected");
                    } else {
                        try {
                            String qrContent = textEncryptor.decrypt(qrContentEncrypted);
                            Map<String, Object> urlFields = hmacService.getUrlFields(config.getConfig().getHmac().getContext(), qrContent);
                            String namespace = (String) urlFields.get("namespace");
                            String name = (String) urlFields.get("name");
                            Integer page = Integer.valueOf((String) urlFields.get("page"));
                            if (StringUtils.isEmpty(namespace)) {
                                throw new PdfQRScanException("The namespage field is empty");
                            }
                            if (StringUtils.isEmpty(name)) {
                                throw new PdfQRScanException("The name field is empty");
                            }
                            if (page == null) {
                                throw new PdfQRScanException("The page field is empty");
                            }
                            StorageResourceRef storageResourceRef = StorageResourceRef.builder().namespace(StorageNamespace.builder().name(namespace).build()).name(name).build();



                        } catch (Exception e) {
                            pdfQRScanPageResultBuilder.status(PdfQRScanPageStatus.QR_FORMAT_ERROR);
                            pdfQRScanPageResultBuilder.description(e.getMessage());
                        }

                    }


                } catch (Exception e) {
                    pdfQRScanPageResultBuilder.status(PdfQRScanPageStatus.PAGE_PROCESS_ERROR);
                    pdfQRScanPageResultBuilder.description(e.getMessage());
                } finally {
                    pdfQRScanPageResultBuilder.pageNumber(pageNumber);
                }


            }

        }

        return pdfQRScanResultBuilder.build();
    }


    private void convertToPdf(InputStreamSource inputStreamSource) {

    }

//    public static File convertTIFFToPDF(File tiffFile)
//    {
//        File pdfFile = new File("C:\\Users\\user\\\\Desktop\\output.pdf");
//        try
//        {
//            RandomAccessFileOrArray myTiffFile = new RandomAccessFileOrArray(tiffFile.getCanonicalPath());
//            // Find number of images in Tiff file
//            int numberOfPages = TiffImage.getNumberOfPages(myTiffFile);
//            Document TifftoPDF = new Document();
//            PdfWriter pdfWriter = PdfWriter.getInstance(TifftoPDF, new FileOutputStream(pdfFile));
//            pdfWriter.setStrictImageSequence(true);
//            TifftoPDF.open();
//            Image tempImage;
//            // Run a for loop to extract images from Tiff file
//            // into a Image object and add to PDF recursively
//            for (int i = 1; i <= numberOfPages; i++) {
//
//                tempImage = TiffImage.getTiffImage(myTiffFile, i);
//                Rectangle pageSize = new Rectangle(tempImage.getWidth(), tempImage.getHeight());
//                TifftoPDF.setPageSize(pageSize);
//                TifftoPDF.newPage();
//                TifftoPDF.add(tempImage);
//            }
//            TifftoPDF.close();
//        }
//        catch(Exception ex)
//        {
//            ex.printStackTrace();
//        }
//
//        return pdfFile;
//    }

//    public PdfQRValidationResult parsePdfWithQRMetadata(MultipartFile pdfFile) {
//        PdfQRValidationResult pdfQrValidationResult = new PdfQRValidationResult();
//
//
//        return pdfQrValidationResult;
//    }
//
//
//    private String getQRCode(PdfReader pdfReader) throws IOException {
//        PdfWriter pdfWriter = new PdfWriter(pdfReader);
//
//
//        BufferedImage bufferedImage = ImageIO.read(inputStream);
//        LuminanceSource luminanceSource = new BufferedImageLuminanceSource(bufferedImage);
//        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
//        try {
//            Result result = new MultiFormatReader().decode(binaryBitmap);
//            return result.getText();
//        } catch (NotFoundException e) {
//            //System.inbound.println("There is no QR code in the image");
//            return null;
//        }
//
//    }
//
//
//    private
//
//
//    public class TiffToPdf {
//        public static void main(String[] args) throws IOException {
//            Path tiffFile = Paths.get("/myfolder/origin.tiff");
//            RandomAccessFileOrArray raf = new RandomAccessFileOrArray(new RandomAccessSourceFactory().createBestSource(tiffFile.toString()));
//            int tiffPages = TiffImageData.getNumberOfPages(raf);
//            raf.close();
//            try (PdfDocument output = new PdfDocument(new PdfWriter("/myfolder/destination.pdf"))) {
//                for (int page = 1; page <= tiffPages; page++) {
//                    ImageData tiffImage = ImageDataFactory.createTiff(tiffFile.toUri().toURL(), true, page, true);
//                    Rectangle tiffPageSize = new Rectangle(tiffImage.getWidth(), tiffImage.getHeight());
//                    PdfPage newPage = output.addNewPage(new PageSize(tiffPageSize));
//                    PdfCanvas canvas = new PdfCanvas(newPage);
//                    canvas.addImage(tiffImage, tiffPageSize, false);
//                }
//            }
//        }
//    }


    private String scanQRCode(PDFRenderer pdfRenderer, int pageNumber) throws NotFoundException, IOException {
        // Hints for scanning
        Vector<BarcodeFormat> decodeFormat = new Vector<>();
        decodeFormat.add(BarcodeFormat.QR_CODE);
        Hashtable<DecodeHintType, Object> hintMap = new Hashtable<>();
        hintMap.put(DecodeHintType.TRY_HARDER, true);
        hintMap.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormat);
        MultiFormatReader qrcodeReader = new MultiFormatReader();
        qrcodeReader.setHints(hintMap);

        // We try for several images of the PDF page at several DPI settings,
        // starting at the lowest setting, this might help for speed...
        int[] dpiSettings = {150, 200, 250, 300};
        for (int i = 0; i < dpiSettings.length; i++) {
            try {
                // Try lowest DPI first.
                BufferedImage pageImage = getPageImage(pdfRenderer, pageNumber, dpiSettings[i]);
                LuminanceSource source = new BufferedImageLuminanceSource(pageImage);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                // By using decodeWithState, we keep the Hints that we set earlier.
                Result result = qrcodeReader.decodeWithState(bitmap);
                return result.getText();
            } catch (NotFoundException e) {
                // Attempt failed. Try next resolution.
                // What if this fails again and again?
                // A NotFoundException is thrown.
                if (i == dpiSettings.length - 1) {
                    throw e;
                }
            }
        }
        // This should never happen, ever...
        return null;
    }

    private BufferedImage getPageImage(PDFRenderer pdfRenderer, int pageNumber, int dpi) throws IOException {
        BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber - 1, dpi, ImageType.BINARY);
        return image;
    }

}
