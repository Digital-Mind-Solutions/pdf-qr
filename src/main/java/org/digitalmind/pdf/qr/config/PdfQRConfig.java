package org.digitalmind.pdf.qr.config;

import lombok.*;
import org.digitalmind.pdf.qr.dto.PdfQRSpecification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.digitalmind.pdf.qr.config.PdfQRModuleConfig.ENABLED;
import static org.digitalmind.pdf.qr.config.PdfQRModuleConfig.PREFIX;

@Configuration
@ConditionalOnProperty(name = ENABLED, havingValue = "true")
@ConfigurationProperties(prefix = PREFIX)
@EnableConfigurationProperties
@Getter
@Setter
public class PdfQRConfig {
    private boolean enabled;
    private PdfQRConfigProperties config;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class PdfQRConfigProperties {
        @Singular
        private Map<String, PdfQRSpecification> specifications;
        private PdfQRHmacProperties hmac;

    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class PdfQRHmacProperties {
        private String context;
        private String base;
        private String fragment;
    }

}
