package com.fabbitinc.server.application.part.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "부품 리비전 수정 요청")
public class UpdatePartRevisionRequest {

    @Schema(description = "품명", example = "M3 볼트")
    @Size(max = 500, message = "name은 최대 500자여야 합니다")
    private String name;

    @Schema(description = "재질", example = "SUS304")
    @Size(max = 200, message = "material은 최대 200자여야 합니다")
    private String material;

    @Schema(description = "단위", example = "EA")
    @Size(max = 20, message = "unit은 최대 20자여야 합니다")
    private String unit;

    @Schema(description = "설명", example = "체결용 표준 부품")
    private String description;

    @Schema(description = "카테고리", example = "FASTENER")
    @Size(max = 100, message = "category는 최대 100자여야 합니다")
    private String category;

    @Schema(description = "팬텀 부품 여부", example = "false")
    private Boolean phantom;

    @Schema(description = "리드타임(일)", example = "7")
    @Min(value = 0, message = "lead_time_days는 0 이상이어야 합니다")
    private Integer leadTimeDays;

    @Schema(description = "확장 속성 JSON 객체", example = "{\"weight\":1.2,\"color\":\"silver\"}")
    private Map<String, Object> extendedProperties;

    private boolean nameSet;
    private boolean materialSet;
    private boolean unitSet;
    private boolean descriptionSet;
    private boolean categorySet;
    private boolean phantomSet;
    private boolean leadTimeDaysSet;
    private boolean extendedPropertiesSet;

    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
        this.nameSet = true;
    }

    public String getMaterial() {
        return material;
    }

    @JsonSetter("material")
    public void setMaterial(String material) {
        this.material = material;
        this.materialSet = true;
    }

    public String getUnit() {
        return unit;
    }

    @JsonSetter("unit")
    public void setUnit(String unit) {
        this.unit = unit;
        this.unitSet = true;
    }

    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionSet = true;
    }

    public String getCategory() {
        return category;
    }

    @JsonSetter("category")
    public void setCategory(String category) {
        this.category = category;
        this.categorySet = true;
    }

    public Boolean getPhantom() {
        return phantom;
    }

    @JsonSetter("is_phantom")
    public void setPhantom(Boolean phantom) {
        this.phantom = phantom;
        this.phantomSet = true;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    @JsonSetter("lead_time_days")
    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
        this.leadTimeDaysSet = true;
    }

    public Map<String, Object> getExtendedProperties() {
        return extendedProperties;
    }

    @JsonSetter("extended_properties")
    public void setExtendedProperties(Map<String, Object> extendedProperties) {
        this.extendedProperties = extendedProperties;
        this.extendedPropertiesSet = true;
    }

    public boolean isNameSet() {
        return nameSet;
    }

    public boolean isMaterialSet() {
        return materialSet;
    }

    public boolean isUnitSet() {
        return unitSet;
    }

    public boolean isDescriptionSet() {
        return descriptionSet;
    }

    public boolean isCategorySet() {
        return categorySet;
    }

    public boolean isPhantomSet() {
        return phantomSet;
    }

    public boolean isLeadTimeDaysSet() {
        return leadTimeDaysSet;
    }

    public boolean isExtendedPropertiesSet() {
        return extendedPropertiesSet;
    }
}
