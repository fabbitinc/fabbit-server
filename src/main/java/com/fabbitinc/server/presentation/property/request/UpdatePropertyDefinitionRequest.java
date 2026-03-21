package com.fabbitinc.server.presentation.property.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "속성 정의 수정 요청")
public class UpdatePropertyDefinitionRequest {

    @Schema(description = "속성 표시명", example = "표면처리")
    @Size(max = 200, message = "display_name은 최대 200자여야 합니다")
    private String displayName;

    @Schema(description = "속성 설명", example = "표면처리 방식")
    private String description;

    @Schema(description = "속성 값 타입", example = "OPTION")
    private PropertyValueType valueType;

    @Schema(description = "옵션 입력 모드", example = "FIXED")
    private PropertyOptionMode optionMode;

    @Schema(description = "옵션 목록")
    @Valid
    private List<PropertyOptionRequest> options;

    @Schema(description = "표시 순서", example = "120")
    @Min(value = 0, message = "display_order는 0 이상이어야 합니다")
    private Integer displayOrder;

    @Schema(description = "필수 여부", example = "false")
    private Boolean required;

    @Schema(description = "활성 여부", example = "true")
    private Boolean active;

    private boolean displayNameSet;
    private boolean descriptionSet;
    private boolean valueTypeSet;
    private boolean optionModeSet;
    private boolean optionsSet;
    private boolean displayOrderSet;
    private boolean requiredSet;
    private boolean activeSet;

    public String getDisplayName() {
        return displayName;
    }

    @JsonSetter("display_name")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.displayNameSet = true;
    }

    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionSet = true;
    }

    public PropertyValueType getValueType() {
        return valueType;
    }

    @JsonSetter("value_type")
    public void setValueType(PropertyValueType valueType) {
        this.valueType = valueType;
        this.valueTypeSet = true;
    }

    public PropertyOptionMode getOptionMode() {
        return optionMode;
    }

    @JsonSetter("option_mode")
    public void setOptionMode(PropertyOptionMode optionMode) {
        this.optionMode = optionMode;
        this.optionModeSet = true;
    }

    public List<PropertyOptionRequest> getOptions() {
        return options;
    }

    @JsonSetter("options")
    public void setOptions(List<PropertyOptionRequest> options) {
        this.options = options;
        this.optionsSet = true;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    @JsonSetter("display_order")
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        this.displayOrderSet = true;
    }

    public Boolean getRequired() {
        return required;
    }

    @JsonSetter("required")
    public void setRequired(Boolean required) {
        this.required = required;
        this.requiredSet = true;
    }

    public Boolean getActive() {
        return active;
    }

    @JsonSetter("active")
    public void setActive(Boolean active) {
        this.active = active;
        this.activeSet = true;
    }

    public boolean isDisplayNameSet() {
        return displayNameSet;
    }

    public boolean isDescriptionSet() {
        return descriptionSet;
    }

    public boolean isValueTypeSet() {
        return valueTypeSet;
    }

    public boolean isOptionModeSet() {
        return optionModeSet;
    }

    public boolean isOptionsSet() {
        return optionsSet;
    }

    public boolean isDisplayOrderSet() {
        return displayOrderSet;
    }

    public boolean isRequiredSet() {
        return requiredSet;
    }

    public boolean isActiveSet() {
        return activeSet;
    }
}
