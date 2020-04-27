package org.digitalmind.pdf.qr.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.digitalmind.buildingblocks.core.jpaauditor.entity.ContextVersionableAuditModel;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@SuperBuilder
@Entity
@Table(
        name = "qr-short-code",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "i18n_ux1",
                        columnNames = {"code", "locale"}
                )
        }
)
@EntityListeners({AuditingEntityListener.class})

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)

@ApiModel(value = "QRShortCode", description = "Entity for providing QRShortCode support.")
@JsonPropertyOrder(
        {
                "id", "code", "content",
                "createdAt", "createdBy", "updatedAt", "updatedBy"
        }
)

public class QRShortCode extends ContextVersionableAuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @ApiModelProperty(value = "Unique id of the qr short code", required = false)
    private Long id;

    @ApiModelProperty(value = "The qr short code", required = true)
    @Column(name = "code")
    @NotNull
    private String code;

    @ApiModelProperty(value = "The qr content", required = true)
    @Column(name = "content")
    @NotNull
    private String content;

}
