package com.umc.devine.domain.project.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum ProjectField {

    WEB("웹"),
    MOBILE("모바일/앱"),
    AI("AI/머신러닝"),
    GAME("게임"),
    DATA("데이터"),
    BACKEND("백엔드"),
    FRONTEND("프론트엔드");

    private final String displayName;

    ProjectField(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator
    public static ProjectField from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ProjectField field : ProjectField.values()) {
            if (field.name().equalsIgnoreCase(value) || field.displayName.equals(value)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Invalid ProjectField: " + value);
    }
}
