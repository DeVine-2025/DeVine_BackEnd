package com.umc.devine.domain.project.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum ProjectMode {

    ONLINE("온라인"),
    OFFLINE("오프라인"),
    HYBRID("온라인/오프라인");   // 온오프라인 병행

    private final String displayName;

    ProjectMode(String displayName) {
        this.displayName = displayName;
    }

    @JsonCreator
    public static ProjectMode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ProjectMode mode : ProjectMode.values()) {
            if (mode.name().equalsIgnoreCase(value) || mode.displayName.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid ProjectMode: " + value);
    }
}
