package com.umc.devine.domain.project.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum ProjectField {

    ALL("전체"),
    WEB("웹"),
    MOBILE("모바일/앱"),
    GAME("게임"),
    BLOCKCHAIN("블록체인"),
    ETC("기타");

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
