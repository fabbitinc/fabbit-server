package com.fabbitinc.server.presentation.bom.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "BOM 항목 수정 요청")
public class UpdateBomItemRequest {

    @Schema(description = "하위 부품 리비전 ID")
    private UUID childPartRevisionId;

    @Schema(description = "BOM 줄 번호", example = "10")
    @Size(max = 50, message = "BOM 줄 번호는 최대 50자여야 합니다")
    private String lineNumber;

    @Schema(description = "수량", example = "2")
    @DecimalMin(value = "0", inclusive = false, message = "수량은 0보다 커야 합니다")
    private BigDecimal quantity;

    @Schema(
            description = "확장 속성 JSON 객체. key는 property_definition.id(UUID)여야 합니다",
            example = "{}"
    )
    private Map<String, Object> extendedProperties;

    private boolean childPartRevisionIdSet;
    private boolean lineNumberSet;
    private boolean quantitySet;
    private boolean extendedPropertiesSet;

    public UUID getChildPartRevisionId() {
        return childPartRevisionId;
    }

    @JsonSetter("child_part_revision_id")
    public void setChildPartRevisionId(UUID childPartRevisionId) {
        this.childPartRevisionId = childPartRevisionId;
        this.childPartRevisionIdSet = true;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    @JsonSetter("line_number")
    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
        this.lineNumberSet = true;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    @JsonSetter("quantity")
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        this.quantitySet = true;
    }

    public Map<String, Object> getExtendedProperties() {
        return extendedProperties;
    }

    @JsonSetter("extended_properties")
    public void setExtendedProperties(Map<String, Object> extendedProperties) {
        this.extendedProperties = extendedProperties;
        this.extendedPropertiesSet = true;
    }

    public boolean isChildPartRevisionIdSet() {
        return childPartRevisionIdSet;
    }

    public boolean isLineNumberSet() {
        return lineNumberSet;
    }

    public boolean isQuantitySet() {
        return quantitySet;
    }

    public boolean isExtendedPropertiesSet() {
        return extendedPropertiesSet;
    }
}
