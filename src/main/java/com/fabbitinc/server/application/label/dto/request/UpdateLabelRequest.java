package com.fabbitinc.server.application.label.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateLabelRequest {

    @Size(min = 1, max = 50, message = "name은 1~50자여야 합니다")
    private String name;

    @Size(max = 200, message = "description은 최대 200자여야 합니다")
    private String description;

    @Pattern(
            regexp = "^#[0-9a-fA-F]{6}$",
            message = "color는 #RRGGBB 형식이어야 합니다"
    )
    private String color;

    private boolean descriptionSet;

    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionSet = true;
    }

    public String getColor() {
        return color;
    }

    @JsonSetter("color")
    public void setColor(String color) {
        this.color = color;
    }

    public boolean isDescriptionSet() {
        return descriptionSet;
    }
}
