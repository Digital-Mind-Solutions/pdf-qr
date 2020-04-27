package org.digitalmind.pdf.qr.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import lombok.*;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "PdfQRSpecification", description = "The specification of a barcode request that qill decorate a pdf.")
@JsonPropertyOrder(
        {
                "format",
                "width", "height",
                "onColor", "offColor",
                "imageType",
                "logo",
                "position",
                "validity"
        }
)
public class PdfQRSpecification extends BarcodeSpecification {
    private PdfQRPosition position;
    private PdfQRValidity validity;

    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    @Data
    @ApiModel(value = "PdfQRValidity", description = "The validity of a QR code.")
    public static class PdfQRValidity {
        private int years;
        private int months;
        private int days;
        private int hours;
        private int minutes;
        private int seconds;
    }

    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    @Data
    @ApiModel(value = "PdfQRPosition", description = "The position of a QR code relative to page margins.")
    public static class PdfQRPosition {
        private PdfQRVAlign vAlign;
        private float vMargin;
        private PdfQRHAlign hAlign;
        private float hMargin;
    }

}
