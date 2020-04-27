package org.digitalmind.pdf.qr.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Data
public class PdfQRValidationResult {
    private boolean valid;
}
